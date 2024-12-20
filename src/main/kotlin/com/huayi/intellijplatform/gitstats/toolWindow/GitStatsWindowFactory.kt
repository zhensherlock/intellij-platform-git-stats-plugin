package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.components.SettingAction
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import com.huayi.intellijplatform.gitstats.utils.Utils
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.michaelbaranov.microba.calendar.DatePicker
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.Font
import java.util.*
import javax.swing.*
import kotlin.concurrent.thread


class GitStatsWindowFactory : ToolWindowFactory {

//    init {
//        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
//    }

    private val contentFactory = ContentFactory.SERVICE.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val gitStatsWindow = GitStatsWindow(toolWindow)
        val content = contentFactory.createContent(gitStatsWindow.getContent(toolWindow), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class GitStatsWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<GitStatsService>()

        fun getContent(toolWindow: ToolWindow) = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            var (startTime, endTime) = Utils.getThisWeekDateTimeRange()
            val settingModel = SettingModel().apply {
                mode = "Top-speed"
                exclude = ""
            }
            val table = JBTable().apply {
                font = Font("Microsoft YaHei", Font.PLAIN, 14)
                tableHeader.font = Font("Microsoft YaHei", Font.BOLD, 14)
                border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
                columnSelectionAllowed = false
                rowSelectionAllowed = true
                rowHeight = 30
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            }
            var refreshButton: JButton
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

            val contentPanel = JBPanel<JBPanel<*>>().apply {
                val tablePanel = JBScrollPane(table).apply {
                    isFocusable = false
                    border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
                }
                add(tablePanel)

                val loadingPanel = JBLoadingPanel(BorderLayout(), toolWindow.project).also {
                    it.startLoading()
                }
                add(loadingPanel)

                layout = CardLayout().also {
                    it.addLayoutComponent(tablePanel, "content_table")
                    it.addLayoutComponent(loadingPanel, "content_loading")
                }
            }
            val headerPanel = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(JBBox.createHorizontalStrut(10))
                add(JBLabel(MyBundle.message("filterStartTimeLabel", "")))
//                add(LocalDateTimePicker(LocalDateTime.now()))
//                datePicker.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//                add(CalendarView())
//                val datePicker = JXDatePicker()
//                datePicker.date = Date()
//                add(DatePicker(Date.from(startTime.atStartOfDay(ZoneId.systemDefault()).toInstant())).apply {
                add(DatePicker(startTime).apply {
                    isDropdownFocusable = false
//                    isFieldEditable = false
                    val size = Dimension(200, preferredSize.height)
                    preferredSize = size
                    maximumSize = size
                    minimumSize = size
                    isShowNoneButton = false
                    isShowNumberOfWeek = true
                    isStripTime = true
                    components.last().preferredSize = Dimension(30, 30)
                    addPropertyChangeListener {
                        if (it.propertyName == "date" && it.newValue != null) {
                            startTime = it.newValue as Date
                        }
                    }
                })
                add(JBBox.createHorizontalStrut(10))
                add(JBLabel(MyBundle.message("filterEndTimeLabel", "")))
                add(DatePicker(endTime).apply {
                    isDropdownFocusable = false
                    val size = Dimension(200, preferredSize.height)
                    preferredSize = size
                    maximumSize = size
                    minimumSize = size
                    isShowNoneButton = false
                    isShowNumberOfWeek = true
                    isStripTime = true
                    components.last().preferredSize = Dimension(30, 30)
                    addPropertyChangeListener {
                        if (it.propertyName == "date" && it.newValue != null) {
                            val calendar = Calendar.getInstance()
                            calendar.time = (it.newValue as Date)
                            calendar[Calendar.HOUR_OF_DAY] = 23
                            calendar[Calendar.MINUTE] = 59
                            calendar[Calendar.SECOND] = 59
                            calendar[Calendar.MILLISECOND] = 999
                            endTime = calendar.time
                        }
                    }
                })
//                add(LoadingButton(MyBundle.message("refreshButtonLabel")))
                refreshButton = JButton(MyBundle.message("refreshButtonLabel"), AllIcons.Actions.Refresh).apply {
                    addActionListener {
                        (contentPanel.layout as CardLayout).show(contentPanel, "content_loading")
                        isEnabled = false
                        text = MyBundle.message("refreshButtonLoadingLabel")
                        thread {
                            if (settingModel.mode === "Top-speed") {
                                table.model = service.getTopSpeedUserStats(startTime, endTime, settingModel)
                            } else {
                                table.model = service.getUserStats(startTime, endTime, settingModel)
                            }
                            SwingUtilities.invokeLater {
                                (contentPanel.layout as CardLayout).show(contentPanel, "content_table")
                                isEnabled = true
                                text = MyBundle.message("refreshButtonLabel")
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
                    refreshButton.doClick()
                }
            actionList.add(settingAction)
            toolWindow.setTitleActions(actionList)
        }
    }
}
