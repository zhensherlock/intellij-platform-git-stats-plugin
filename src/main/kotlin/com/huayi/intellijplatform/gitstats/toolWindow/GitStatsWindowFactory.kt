package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import com.huayi.intellijplatform.gitstats.utils.Utils
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.michaelbaranov.microba.calendar.DatePicker
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.Font
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.ListSelectionModel


class GitStatsWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    private val contentFactory = ContentFactory.SERVICE.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val gitStatsWindow = GitStatsWindow(toolWindow)
        val content = contentFactory.createContent(gitStatsWindow.getContent(toolWindow), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class GitStatsWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<GitStatsService>()

        private fun getUserStatsTableModel(startTime: Date, endTime: Date): StatsTableModel {
            val userStats = service.getUserStats(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime)
            )
            val data = userStats.map { item ->
                arrayOf(
                    item.author,
                    item.commitCount.toString(),
                    item.addedLines.toString(),
                    item.deletedLines.toString(),
                    item.modifiedFileCount.toString()
                )
            }.toTypedArray()
            return StatsTableModel(
                data,
                arrayOf("Author", "CommitCount", "AddedLines", "DeletedLines", "ModifiedFileCount")
            )
        }

        fun getContent(toolWindow: ToolWindow) = JBPanel<JBPanel<*>>().apply {
            var (startTime, endTime) = Utils.getThisWeekDateTimeRange()
            val table = JBTable().apply {
                font = Font("Arial", Font.PLAIN, 14)
                tableHeader.font = Font("Arial", Font.BOLD, 14)
                border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
                columnSelectionAllowed = false
                rowSelectionAllowed = true
                rowHeight = 30
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            }
            layout = BorderLayout()
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
//                val dateTimePicker = LocalDateTimePicker(LocalDateTime.now())
//                add(dateTimePicker)
//                add(DatePicker(Date.from(startTime.atStartOfDay(ZoneId.systemDefault()).toInstant())).apply {
                add(DatePicker(startTime).apply {
                    isDropdownFocusable = false
//                    isFieldEditable = false
                    val size = Dimension(200, preferredSize.height)
                    preferredSize = size
                    maximumSize = size
                    minimumSize = size
                    addPropertyChangeListener {
                        if (it.propertyName == "date" && it.newValue != null) {
                            startTime = it.newValue as Date
                        }
                    }
                })
                add(JBBox.createHorizontalStrut(10))
//                datePicker.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//                add(CalendarView())
//                val datePicker = JXDatePicker()
//                datePicker.date = Date()
                add(JBLabel(MyBundle.message("filterEndTimeLabel", "")))
//                add(DatePicker(Date.from(endTime.atStartOfDay(ZoneId.systemDefault()).toInstant())).apply {
                add(DatePicker(endTime).apply {
                    isDropdownFocusable = false
//                    isFieldEditable = false
                    val size = Dimension(200, preferredSize.height)
                    preferredSize = size
                    maximumSize = size
                    minimumSize = size
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
                add(JButton(MyBundle.message("refreshButtonLabel")).apply {
                    addActionListener {
                        (contentPanel.layout as CardLayout).show(contentPanel, "content_loading")
                        table.model = getUserStatsTableModel(startTime, endTime)
                        (contentPanel.layout as CardLayout).show(contentPanel, "content_table")
//                        label.text = MyBundle.message("randomLabel", service.getRandomNumber())
                    }
                })
//                val datePicker = JXDatePicker()
//                datePicker.setFormats(SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
//                datePicker.timeZone = TimeZone.getDefault()
//                datePicker.editor.isEditable = true
//                add(datePicker)
//                datePicker.editor = DateEditor(datePicker, "yyyy-MM-dd HH:mm:ss")
            }
            add(headerPanel, BorderLayout.NORTH)

            add(contentPanel, BorderLayout.CENTER)
        }
    }
}
