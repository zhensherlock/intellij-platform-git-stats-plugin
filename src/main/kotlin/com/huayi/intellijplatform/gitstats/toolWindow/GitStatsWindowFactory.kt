package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.components.RefreshButton
import com.huayi.intellijplatform.gitstats.components.SettingAction
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.huayi.intellijplatform.gitstats.services.GitStatsSettingsService
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import com.huayi.intellijplatform.gitstats.services.GitStatsResult
import com.huayi.intellijplatform.gitstats.utils.Utils
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.*
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.CalendarView
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
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
            var (startTime, endTime) = Utils.getThisWeekDateTimeRange()
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
                summaryLabel.text = createSummaryText(model, visibleRows(table, model))
            }

            fun applyAuthorFilter() {
                val sorter = activeSorter ?: return
                val model = sorter.model
                sorter.rowFilter = createAuthorFilter(model, activeAuthorFilter)
                updateSummary()
            }

            fun configureTableModel(model: StatsTableModel) {
                table.model = model
                val sorter = TableRowSorter(model)
                table.rowSorter = sorter
                activeSorter = sorter
                configureColumns(table, model)
                applyDefaultSort(sorter, model)
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
                        configureTableModel(result.model)
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
                                if (settingModel.mode == SettingModel.MODE_FAST_SUMMARY) {
                                    service.getTopSpeedUserStats(startTime, endTime, settingModel)
                                } else {
                                    service.getUserStats(startTime, endTime, settingModel)
                                }
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

            private fun createSummaryText(model: StatsTableModel, rows: List<StatsTableRow>): String {
                if (rows.isEmpty()) {
                    return MyBundle.message("statsSummaryNoRows")
                }
                val formatter = NumberFormat.getIntegerInstance()
                val authors = formatter.format(rows.size)
                val addedLines = formatter.format(rows.sumOf { it.addedLines })
                val deletedLines = formatter.format(rows.sumOf { it.deletedLines })
                val modifiedFiles = formatter.format(rows.sumOf { it.modifiedFileCount })
                if (model.hasColumn(StatsTableColumnKind.COMMITS)) {
                    return MyBundle.message(
                        "statsSummaryDetailed",
                        authors,
                        formatter.format(rows.sumOf { it.commitCount ?: 0 }),
                        addedLines,
                        deletedLines,
                        modifiedFiles
                    )
                }
                return MyBundle.message(
                    "statsSummaryFast",
                    authors,
                    addedLines,
                    deletedLines,
                    modifiedFiles
                )
            }

            private fun visibleRows(table: JTable, model: StatsTableModel): List<StatsTableRow> {
                return (0 until table.rowCount).map { viewRow ->
                    model.rowAt(table.convertRowIndexToModel(viewRow))
                }
            }

            private fun createAuthorFilter(
                model: StatsTableModel,
                authorFilter: String
            ): RowFilter<StatsTableModel, Int>? {
                if (authorFilter.isBlank()) {
                    return null
                }
                val authorColumn = model.columnIndex(StatsTableColumnKind.AUTHOR)
                if (authorColumn < 0) {
                    return null
                }
                val pattern = Pattern.compile(Pattern.quote(authorFilter), Pattern.CASE_INSENSITIVE)
                return object : RowFilter<StatsTableModel, Int>() {
                    override fun include(entry: Entry<out StatsTableModel, out Int>): Boolean {
                        return pattern.matcher(entry.getStringValue(authorColumn)).find()
                    }
                }
            }

            private fun applyDefaultSort(sorter: TableRowSorter<StatsTableModel>, model: StatsTableModel) {
                val defaultColumn = if (model.hasColumn(StatsTableColumnKind.COMMITS)) {
                    model.columnIndex(StatsTableColumnKind.COMMITS)
                } else {
                    model.columnIndex(StatsTableColumnKind.LINES_ADDED)
                }
                if (defaultColumn >= 0) {
                    sorter.sortKeys = listOf(RowSorter.SortKey(defaultColumn, SortOrder.DESCENDING))
                }
            }

            private fun configureColumns(table: JTable, model: StatsTableModel) {
                val numericRenderer = DefaultTableCellRenderer().apply {
                    horizontalAlignment = SwingConstants.RIGHT
                }
                model.columns.forEachIndexed { modelIndex, column ->
                    val viewIndex = table.convertColumnIndexToView(modelIndex)
                    if (viewIndex < 0) return@forEachIndexed
                    val tableColumn = table.columnModel.getColumn(viewIndex)
                    when (column.kind) {
                        StatsTableColumnKind.AUTHOR -> {
                            tableColumn.minWidth = 160
                            tableColumn.preferredWidth = 260
                        }
                        StatsTableColumnKind.COMMITS -> {
                            tableColumn.minWidth = 80
                            tableColumn.preferredWidth = 90
                            tableColumn.cellRenderer = numericRenderer
                        }
                        StatsTableColumnKind.LINES_ADDED,
                        StatsTableColumnKind.LINES_DELETED,
                        StatsTableColumnKind.MODIFIED_FILES -> {
                            tableColumn.minWidth = 110
                            tableColumn.preferredWidth = 130
                            tableColumn.cellRenderer = numericRenderer
                        }
                    }
                }
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
