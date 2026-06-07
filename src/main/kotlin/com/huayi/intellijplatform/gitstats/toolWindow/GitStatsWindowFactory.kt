package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.components.RefreshButton
import com.huayi.intellijplatform.gitstats.components.SettingAction
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.huayi.intellijplatform.gitstats.services.GitStatsSettingsService
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import com.huayi.intellijplatform.gitstats.utils.Utils
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.*
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.CalendarView
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.text.ParseException
import java.text.SimpleDateFormat
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
        private val settingsService = toolWindow.project.service<GitStatsSettingsService>()

        fun getContent(toolWindow: ToolWindow) = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            var (startTime, endTime) = Utils.getThisWeekDateTimeRange()
            val settingModel = settingsService.getSettings()
            val table = JBTable().apply {
                font = Font("Microsoft YaHei", Font.PLAIN, 14)
                tableHeader.font = Font("Microsoft YaHei", Font.BOLD, 14)
                border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
                columnSelectionAllowed = false
                rowSelectionAllowed = true
                rowHeight = 30
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            }
            var refreshButton: RefreshButton
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

            val loadingPanel = JBLoadingPanel(BorderLayout(), toolWindow.project)
            val contentPanel = JBPanel<JBPanel<*>>().apply {
                val tablePanel = JBScrollPane(table).apply {
                    isFocusable = false
                    border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
                }
                add(tablePanel)

                add(loadingPanel)

                layout = CardLayout().also {
                    it.addLayoutComponent(tablePanel, "content_table")
                    it.addLayoutComponent(loadingPanel, "content_loading")
                }
            }
            val headerPanel = createDateFilterRow().apply {
                fun setDateRange(start: Date, end: Date) {
                    val (normalizedStart, normalizedEnd) = orderedRange(startOfDay(start), endOfDay(end))
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
                        (contentPanel.layout as CardLayout).show(contentPanel, "content_loading")
                        thread {
                            try {
                                val statsModel = if (settingModel.mode == SettingModel.MODE_FAST_SUMMARY) {
                                    service.getTopSpeedUserStats(startTime, endTime, settingModel)
                                } else {
                                    service.getUserStats(startTime, endTime, settingModel)
                                }
                                SwingUtilities.invokeLater {
                                    table.model = statsModel
                                    loadingPanel.stopLoading()
                                    (contentPanel.layout as CardLayout).show(contentPanel, "content_table")
                                    stopLoading()
                                }
                            } catch (throwable: Throwable) {
                                thisLogger().warn("Failed to refresh Git stats", throwable)
                                SwingUtilities.invokeLater {
                                    loadingPanel.stopLoading()
                                    (contentPanel.layout as CardLayout).show(contentPanel, "content_table")
                                    stopLoading()
                                }
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

        private class DateRangeSelectionField(
            initialStartDate: Date,
            initialEndDate: Date,
            parentDisposable: Disposable,
            private val onRangeChanged: (Date, Date) -> Unit,
        ) : ExtendableTextField() {

            private var selectedStartDate = startOfDay(initialStartDate)
            private var selectedEndDate = endOfDay(initialEndDate)

            init {
                isEditable = true
                toolTipText = MyBundle.message("datePickerInputTooltip")
                addBrowseExtension({ showRangePopup() }, parentDisposable)
                addActionListener {
                    commitText()
                }
                addFocusListener(object : FocusAdapter() {
                    override fun focusLost(e: FocusEvent) {
                        commitText()
                    }
                })
                setRange(selectedStartDate, selectedEndDate)
                setFixedWidth(248)
            }

            fun setRange(startDate: Date, endDate: Date) {
                val (start, end) = orderedRange(startOfDay(startDate), endOfDay(endDate))
                selectedStartDate = start
                selectedEndDate = end
                text = "${formatDate(selectedStartDate)} - ${formatDate(selectedEndDate)}"
                clearError()
            }

            fun commitText(): Boolean {
                val parsedRange = parseDateRange(text)
                if (parsedRange == null) {
                    showError()
                    return false
                }
                val (start, end) = parsedRange
                setRange(start, end)
                onRangeChanged(selectedStartDate, selectedEndDate)
                return true
            }

            private fun showRangePopup() {
                commitText()
                val startCalendarView = CalendarView(CalendarView.Mode.DATE).apply {
                    setDate(selectedStartDate)
                }
                val endCalendarView = CalendarView(CalendarView.Mode.DATE).apply {
                    setDate(selectedEndDate)
                }
                var popup: JBPopup? = null

                fun applyRange(startDate: Date, endDate: Date) {
                    val (start, end) = orderedRange(startOfDay(startDate), endOfDay(endDate))
                    setRange(start, end)
                    onRangeChanged(start, end)
                    popup?.closeOk(null)
                }

                val applyButton = JButton(MyBundle.message("datePickerApplyButton")).apply {
                    addActionListener {
                        applyRange(startCalendarView.date, endCalendarView.date)
                    }
                }

                val presetPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(createPresetButton(MyBundle.message("datePickerThisWeekButton")) {
                        val (start, end) = Utils.getThisWeekDateTimeRange()
                        applyRange(start, end)
                    })
                    add(JBBox.createHorizontalStrut(6))
                    add(createPresetButton(MyBundle.message("datePickerLast7DaysButton")) {
                        val (start, end) = getLastSevenDaysDateRange()
                        applyRange(start, end)
                    })
                    add(JBBox.createHorizontalStrut(6))
                    add(createPresetButton(MyBundle.message("datePickerThisMonthButton")) {
                        val (start, end) = getThisMonthDateRange()
                        applyRange(start, end)
                    })
                }

                startCalendarView.registerEnterHandler {
                    applyButton.doClick()
                }
                endCalendarView.registerEnterHandler {
                    applyButton.doClick()
                }

                val calendarsPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(startCalendarView)
                    add(JBBox.createHorizontalStrut(12))
                    add(endCalendarView)
                }
                val actionsPanel = JPanel(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(8, 0, 0, 0)
                    add(presetPanel, BorderLayout.WEST)
                    add(applyButton, BorderLayout.EAST)
                }
                val popupContent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    add(calendarsPanel, BorderLayout.CENTER)
                    add(actionsPanel, BorderLayout.SOUTH)
                }

                popup = JBPopupFactory.getInstance()
                    .createComponentPopupBuilder(popupContent, startCalendarView.daysCombo)
                    .setRequestFocus(true)
                    .setFocusable(true)
                    .setCancelOnClickOutside(true)
                    .createPopup()
                popup.showUnderneathOf(this)
            }

            private fun createPresetButton(text: String, action: () -> Unit) = JButton(text).apply {
                addActionListener {
                    action()
                }
            }

            private fun showError() {
                putClientProperty("JComponent.outline", "error")
                toolTipText = MyBundle.message("datePickerInvalidRange")
                repaint()
            }

            private fun clearError() {
                putClientProperty("JComponent.outline", null)
                toolTipText = MyBundle.message("datePickerInputTooltip")
                repaint()
            }
        }

        companion object {
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

            private fun formatDate(date: Date) = SimpleDateFormat("yyyy-MM-dd").format(date)

            private fun parseDateRange(value: String): Pair<Date, Date>? {
                val match = DATE_RANGE_PATTERN.matchEntire(value.trim()) ?: return null
                val start = parseDate(match.groupValues[1]) ?: return null
                val end = parseDate(match.groupValues[2]) ?: return null
                return orderedRange(startOfDay(start), endOfDay(end))
            }

            private fun parseDate(value: String): Date? {
                return try {
                    SimpleDateFormat("yyyy-MM-dd").apply {
                        isLenient = false
                    }.parse(value)
                } catch (_: ParseException) {
                    null
                }
            }

            private fun orderedRange(startDate: Date, endDate: Date): Pair<Date, Date> {
                return if (startDate.after(endDate)) {
                    Pair(startOfDay(endDate), endOfDay(startDate))
                } else {
                    Pair(startDate, endDate)
                }
            }

            private fun startOfDay(date: Date): Date {
                return Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
            }

            private fun endOfDay(date: Date): Date {
                return Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.time
            }

            private fun getLastSevenDaysDateRange(): Pair<Date, Date> {
                val endCalendar = Calendar.getInstance()
                val startCalendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -6)
                }
                return Pair(startOfDay(startCalendar.time), endOfDay(endCalendar.time))
            }

            private fun getThisMonthDateRange(): Pair<Date, Date> {
                val calendar = Calendar.getInstance()
                val startCalendar = calendar.clone() as Calendar
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
                val endCalendar = calendar.clone() as Calendar
                endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                return Pair(startOfDay(startCalendar.time), endOfDay(endCalendar.time))
            }

            private val DATE_RANGE_PATTERN = Regex("""^(\d{4}-\d{2}-\d{2})\s*(?:-|~|to|至)\s*(\d{4}-\d{2}-\d{2})$""")
        }
    }
}
