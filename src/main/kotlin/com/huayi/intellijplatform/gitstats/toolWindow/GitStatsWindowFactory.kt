package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.components.branchScope.BranchScopeAction
import com.huayi.intellijplatform.gitstats.components.SettingAction
import com.huayi.intellijplatform.gitstats.components.filters.AuthorFilterAction
import com.huayi.intellijplatform.gitstats.components.filters.DateRangeFilterAction
import com.huayi.intellijplatform.gitstats.components.filters.RefreshStatsAction
import com.huayi.intellijplatform.gitstats.models.BranchScope
import com.huayi.intellijplatform.gitstats.services.GitStatsSettingsService
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import com.huayi.intellijplatform.gitstats.services.GitStatsResult
import com.huayi.intellijplatform.gitstats.utils.DateRanges
import com.huayi.intellijplatform.gitstats.utils.GitDataResult
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.*
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
            var startTime: Date? = null
            var endTime: Date? = null
            val settingModel = settingsService.getSettings()
            var activeBranchScope: BranchScope = BranchScope.CurrentBranch
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
            lateinit var refreshAction: RefreshStatsAction
            var authorFilterAction: AuthorFilterAction? = null
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
                table.rowSorter = null
                activeSorter = null
                table.model = model
                val sorter = TableRowSorter(model)
                table.rowSorter = sorter
                activeSorter = sorter
                authorFilterAction?.setAvailableAuthors(model.rows.map { it.author })
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
                authorFilterAction?.setAvailableAuthors(emptyList())
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

            fun setDateRange(start: Date?, end: Date?) {
                if (start == null || end == null) {
                    startTime = null
                    endTime = null
                    return
                }

                val (normalizedStart, normalizedEnd) = DateRanges.orderedRange(
                    DateRanges.startOfDay(start),
                    DateRanges.endOfDay(end)
                )
                startTime = normalizedStart
                endTime = normalizedEnd
            }

            var refreshPending = false
            fun requestStatsRefresh() {
                if (refreshAction.isLoading) {
                    refreshPending = true
                } else {
                    refreshAction.requestRefresh()
                }
            }

            val dateRangeAction = DateRangeFilterAction(startTime, endTime) { start, end ->
                setDateRange(start, end)
                requestStatsRefresh()
            }
            lateinit var filterToolbar: ActionToolbar
            val branchScopeAction = BranchScopeAction(toolWindow.project) { scope ->
                activeBranchScope = scope
                filterToolbar.updateActionsAsync()
                requestStatsRefresh()
            }
            authorFilterAction = AuthorFilterAction { author ->
                activeAuthorFilter = author
                applyAuthorFilter()
            }
            lateinit var refreshToolbar: ActionToolbar
            refreshAction = RefreshStatsAction { action ->
                action.startLoading()
                refreshToolbar.updateActionsAsync()
                loadingPanel.startLoading()
                contentLayout.show(contentPanel, CONTENT_LOADING)
                thread {
                    val result = try {
                        service.getStats(startTime, endTime, settingModel, activeBranchScope)
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
                        action.stopLoading()
                        refreshToolbar.updateActionsAsync()
                        if (refreshPending) {
                            refreshPending = false
                            requestStatsRefresh()
                        } else {
                            renderResult(result)
                        }
                    }
                }
            }
            filterToolbar = createToolbar(
                ACTION_PLACE_FILTER_TOOLBAR,
                DefaultActionGroup().apply {
                    add(dateRangeAction)
                    add(branchScopeAction)
                    add(authorFilterAction!!)
                }
            )
            refreshToolbar = createToolbar(
                ACTION_PLACE_REFRESH_TOOLBAR,
                DefaultActionGroup(refreshAction)
            )
            val headerPanel = createFilterToolbarPanel(filterToolbar, refreshToolbar)
            thread(isDaemon = true, name = "GitStats branch scope loader") {
                when (val branchInfoResult = service.getBranchInfo()) {
                    is GitDataResult.Failure -> thisLogger().debug(
                        "Unable to load Git branch scope options: ${branchInfoResult.details.orEmpty()}"
                    )

                    is GitDataResult.Success -> SwingUtilities.invokeLater {
                        branchScopeAction.setBranchInfo(branchInfoResult.data)
                        filterToolbar.updateActionsAsync()
                    }
                }
            }
            requestStatsRefresh()
            add(headerPanel, BorderLayout.NORTH)

            add(contentPanel, BorderLayout.CENTER)

            val actionList: MutableList<AnAction> = ArrayList()
            val settingAction =
                SettingAction(MyBundle.message("settingButtonTooltipText"), settingModel) { value ->
                    settingModel.mode = value.mode
                    settingModel.exclude = value.exclude
                    settingsService.updateSettings(settingModel)
                    requestStatsRefresh()
                }
            actionList.add(settingAction)
            toolWindow.setTitleActions(actionList)
        }

        companion object {
            private const val CONTENT_TABLE = "content_table"
            private const val CONTENT_LOADING = "content_loading"
            private const val CONTENT_STATE = "content_state"
            private const val NOTIFICATION_GROUP_ID = "GitStats"
            private const val ACTION_PLACE_FILTER_TOOLBAR = "GitStats.FilterToolbar"
            private const val ACTION_PLACE_REFRESH_TOOLBAR = "GitStats.RefreshToolbar"

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

            private fun createToolbar(place: String, actionGroup: DefaultActionGroup): ActionToolbar {
                return ActionManager.getInstance().createActionToolbar(place, actionGroup, true).apply {
                    setReservePlaceAutoPopupIcon(false)
                    component.isOpaque = false
                }
            }

            private fun createFilterToolbarPanel(
                filterToolbar: ActionToolbar,
                refreshToolbar: ActionToolbar
            ) = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = BorderFactory.createEmptyBorder(2, 10, 2, 10)
                maximumSize = Dimension(Int.MAX_VALUE, 36)
                filterToolbar.targetComponent = this
                refreshToolbar.targetComponent = this
                add(filterToolbar.component, BorderLayout.WEST)
                add(refreshToolbar.component, BorderLayout.EAST)
            }

        }
    }
}
