package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.components.DateRangeSelectionField
import com.huayi.intellijplatform.gitstats.components.RefreshButton
import com.huayi.intellijplatform.gitstats.components.SettingAction
import com.huayi.intellijplatform.gitstats.services.GitStatsSettingsService
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import com.huayi.intellijplatform.gitstats.services.GitStatsResult
import com.huayi.intellijplatform.gitstats.utils.DateRanges
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.*
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.TableRowSorter
import kotlin.concurrent.thread


class GitStatsWindowFactory : ToolWindowFactory {

//    init {
//        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
//    }

    private val contentFactory = ContentFactory.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val gitStatsWindow = GitStatsWindow(toolWindow)
        val content = contentFactory.createContent(gitStatsWindow.getContent(toolWindow), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class GitStatsWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<GitStatsService>()
        private val settingsService = toolWindow.project.service<GitStatsSettingsService>()

        fun getContent(toolWindow: ToolWindow) = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            var (startTime, endTime) = DateRanges.thisWeekDateTimeRange()
            val settingModel = settingsService.getSettings()
            var activeSorter: TableRowSorter<StatsTableModel>? = null
            var activeAuthorFilter = ""
            val table = JBTable(StatsTableModel.empty()).apply {
                font = Font("Microsoft YaHei", Font.PLAIN, 14)
                tableHeader.font = Font("Microsoft YaHei", Font.BOLD, 14)
                border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
                columnSelectionAllowed = false
                rowSelectionAllowed = true
                rowHeight = 30
                fillsViewportHeight = true
                setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            }
            StatsTableActions.install(table, toolWindow.project)
            var refreshButton: RefreshButton
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

            val loadingPanel = JBLoadingPanel(BorderLayout(), toolWindow.project)
            val tablePanel = JBScrollPane(table).apply {
                isFocusable = false
                border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
            }
            val summaryLabel = JBLabel(MyBundle.message("statsSummaryNoRows")).apply {
                foreground = UIUtil.getContextHelpForeground()
            }
            val tableContentPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(tablePanel, BorderLayout.CENTER)
                add(createSummaryPanel(summaryLabel), BorderLayout.SOUTH)
            }
            val stateTitleLabel = JBLabel("", SwingConstants.CENTER).apply {
                font = font.deriveFont(Font.BOLD, 15f)
                alignmentX = Component.CENTER_ALIGNMENT
            }
            val stateMessageLabel = JBLabel("", SwingConstants.CENTER).apply {
                foreground = UIUtil.getContextHelpForeground()
                alignmentX = Component.CENTER_ALIGNMENT
            }
            val statePanel = createStatePanel(stateTitleLabel, stateMessageLabel)
            val contentLayout = CardLayout()
            val contentPanel = JBPanel<JBPanel<*>>(contentLayout).apply {
                add(tableContentPanel, CONTENT_TABLE)
                add(loadingPanel, CONTENT_LOADING)
                add(statePanel, CONTENT_STATE)
            }

            fun updateSummary() {
                val model = table.model as? StatsTableModel ?: StatsTableModel.empty()
                summaryLabel.text = StatsTableSupport.createSummaryText(
                    model,
                    StatsTableSupport.visibleRows(table, model)
                )
            }

            fun applyAuthorFilter() {
                val sorter = activeSorter ?: return
                val model = sorter.model
                sorter.rowFilter = StatsTableSupport.createAuthorFilter(model, activeAuthorFilter)
                updateSummary()
            }

            fun configureTableModel(model: StatsTableModel) {
                table.model = model
                val sorter = TableRowSorter(model)
                table.rowSorter = sorter
                activeSorter = sorter
                StatsTableSupport.configureColumns(table, model)
                StatsTableSupport.applyDefaultSort(sorter, model)
                applyAuthorFilter()
                sorter.addRowSorterListener {
                    updateSummary()
                }
                updateSummary()
            }

            fun showState(title: String, message: String) {
                table.rowSorter = null
                activeSorter = null
                table.model = StatsTableModel.empty()
                updateSummary()
                stateTitleLabel.text = title
                stateMessageLabel.text = htmlCenter(message)
                contentLayout.show(contentPanel, CONTENT_STATE)
            }

            fun renderResult(result: GitStatsResult) {
                when (result) {
                    is GitStatsResult.Success -> {
                        configureTableModel(StatsTableModelFactory.fromReport(result.report))
                        contentLayout.show(contentPanel, CONTENT_TABLE)
                    }

                    is GitStatsResult.Empty -> showState(result.title, result.message)

                    is GitStatsResult.Failure -> {
                        showState(result.title, result.message)
                        result.details?.let { thisLogger().warn("Git Stats status details:\n$it") }
                        if (result.notify) {
                            notifyFailure(toolWindow.project, result)
                        }
                    }
                }
            }

            val headerPanel = createDateFilterRow().apply {
                fun setDateRange(start: Date, end: Date) {
                    val (normalizedStart, normalizedEnd) = DateRanges.orderedRange(
                        DateRanges.startOfDay(start),
                        DateRanges.endOfDay(end)
                    )
                    startTime = normalizedStart
                    endTime = normalizedEnd
                }

                val dateRangeField = DateRangeSelectionField(startTime, endTime, toolWindow.disposable) { start, end ->
                    setDateRange(start, end)
                }
                add(JBLabel(MyBundle.message("filterDateRangeLabel")))
                add(JBBox.createHorizontalStrut(6))
                add(dateRangeField)
                add(JBBox.createHorizontalStrut(10))
                add(JBLabel(MyBundle.message("filterAuthorLabel")))
                add(JBBox.createHorizontalStrut(6))
                add(ExtendableTextField().apply {
                    emptyText.text = MyBundle.message("filterAuthorPlaceholder")
                    setFixedWidth(170)
                    onTextChanged {
                        activeAuthorFilter = text.trim()
                        applyAuthorFilter()
                    }
                })
                add(JBBox.createHorizontalStrut(10))
                refreshButton = RefreshButton(
                    MyBundle.message("refreshButtonLabel"),
                    MyBundle.message("refreshButtonLoadingLabel"),
                ).apply {
                    addActionListener {
                        if (!dateRangeField.commitText()) {
                            dateRangeField.requestFocusInWindow()
                            return@addActionListener
                        }
                        startLoading()
                        loadingPanel.startLoading()
                        contentLayout.show(contentPanel, CONTENT_LOADING)
                        thread {
                            val result = try {
                                service.getStats(startTime, endTime, settingModel)
                            } catch (throwable: Throwable) {
                                thisLogger().warn("Failed to refresh Git stats", throwable)
                                GitStatsResult.Failure(
                                    MyBundle.message("stateUnexpectedErrorTitle"),
                                    MyBundle.message(
                                        "stateUnexpectedErrorMessage",
                                        throwable.message ?: throwable::class.java.simpleName
                                    ),
                                    throwable.stackTraceToString()
                                )
                            }
                            SwingUtilities.invokeLater {
                                loadingPanel.stopLoading()
                                stopLoading()
                                renderResult(result)
                            }
                        }
                    }
                    doClick()
                }
                add(refreshButton)
                add(Box.createHorizontalGlue())
//                add(IconLabelButton(AllIcons.General.Settings) {
//                    SettingDialogWrapper().showAndGet()
//                }.apply {
//                    toolTipText = MyBundle.message("settingButtonTooltipText")
//                })
//                add(JBBox.createHorizontalStrut(10))
            }
            add(headerPanel, BorderLayout.NORTH)

            add(contentPanel, BorderLayout.CENTER)

            val actionList: MutableList<AnAction> = ArrayList()
            val settingAction =
                SettingAction(MyBundle.message("settingButtonTooltipText"), settingModel) { value ->
                    settingModel.mode = value.mode
                    settingModel.exclude = value.exclude
                    settingsService.updateSettings(settingModel)
                    refreshButton.doClick()
                }
            actionList.add(settingAction)
            toolWindow.setTitleActions(actionList)
        }

        companion object {
            private const val CONTENT_TABLE = "content_table"
            private const val CONTENT_LOADING = "content_loading"
            private const val CONTENT_STATE = "content_state"
            private const val NOTIFICATION_GROUP_ID = "GitStats"

            private fun notifyFailure(project: Project, result: GitStatsResult.Failure) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification(result.title, result.message, NotificationType.ERROR)
                    .notify(project)
            }

            private fun createStatePanel(titleLabel: JBLabel, messageLabel: JBLabel) =
                JBPanel<JBPanel<*>>(GridBagLayout()).apply {
                    val content = JBPanel<JBPanel<*>>().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        border = BorderFactory.createEmptyBorder(0, 24, 0, 24)
                        add(titleLabel)
                        add(JBBox.createVerticalStrut(8))
                        add(messageLabel)
                    }
                    add(content, GridBagConstraints())
                }

            private fun createSummaryPanel(summaryLabel: JBLabel) =
                JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(6, 10, 6, 10)
                    add(summaryLabel, BorderLayout.WEST)
                }

            private fun htmlCenter(message: String): String {
                return "<html><div style=\"width:420px;text-align:center;\">" +
                    StringUtil.escapeXmlEntities(message) +
                    "</div></html>"
            }

            private fun createDateFilterRow() = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                border = BorderFactory.createEmptyBorder(2, 10, 2, 10)
                maximumSize = Dimension(Int.MAX_VALUE, 34)
            }

            private fun JComponent.setFixedWidth(width: Int) {
                val fixedSize = Dimension(width, preferredSize.height)
                preferredSize = fixedSize
                maximumSize = fixedSize
                minimumSize = fixedSize
            }

            private fun JTextField.onTextChanged(action: () -> Unit) {
                document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent) {
                        action()
                    }

                    override fun removeUpdate(e: DocumentEvent) {
                        action()
                    }

                    override fun changedUpdate(e: DocumentEvent) {
                        action()
                    }
                })
            }

        }
    }
}
