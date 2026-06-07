package com.huayi.intellijplatform.gitstats.components

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.utils.DateRanges
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.CalendarView
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent

class DateRangeSelectionField(
    initialStartDate: Date,
    initialEndDate: Date,
    parentDisposable: Disposable,
    private val onRangeChanged: (Date, Date) -> Unit,
) : ExtendableTextField() {

    private var selectedStartDate = DateRanges.startOfDay(initialStartDate)
    private var selectedEndDate = DateRanges.endOfDay(initialEndDate)

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
        val (start, end) = DateRanges.orderedRange(DateRanges.startOfDay(startDate), DateRanges.endOfDay(endDate))
        selectedStartDate = start
        selectedEndDate = end
        text = "${DateRanges.formatDate(selectedStartDate)} - ${DateRanges.formatDate(selectedEndDate)}"
        clearError()
    }

    fun commitText(): Boolean {
        val parsedRange = DateRanges.parseDateRange(text)
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
            val (start, end) = DateRanges.orderedRange(DateRanges.startOfDay(startDate), DateRanges.endOfDay(endDate))
            setRange(start, end)
            onRangeChanged(start, end)
            popup?.closeOk(null)
        }

        val applyButton = JButton(MyBundle.message("datePickerApplyButton")).apply {
            addActionListener {
                applyRange(startCalendarView.date, endCalendarView.date)
            }
        }

        val presetPanel = JPanelWithHorizontalLayout().apply {
            add(createPresetButton(MyBundle.message("datePickerThisWeekButton")) {
                val (start, end) = DateRanges.thisWeekDateTimeRange()
                applyRange(start, end)
            })
            add(JBBox.createHorizontalStrut(6))
            add(createPresetButton(MyBundle.message("datePickerLast7DaysButton")) {
                val (start, end) = DateRanges.lastSevenDaysDateRange()
                applyRange(start, end)
            })
            add(JBBox.createHorizontalStrut(6))
            add(createPresetButton(MyBundle.message("datePickerThisMonthButton")) {
                val (start, end) = DateRanges.thisMonthDateRange()
                applyRange(start, end)
            })
        }

        startCalendarView.registerEnterHandler {
            applyButton.doClick()
        }
        endCalendarView.registerEnterHandler {
            applyButton.doClick()
        }

        val calendarsPanel = JPanelWithHorizontalLayout().apply {
            add(startCalendarView)
            add(JBBox.createHorizontalStrut(12))
            add(endCalendarView)
        }
        val actionsPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
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

    private fun JComponent.setFixedWidth(width: Int) {
        val fixedSize = Dimension(width, preferredSize.height)
        preferredSize = fixedSize
        maximumSize = fixedSize
        minimumSize = fixedSize
    }

    private class JPanelWithHorizontalLayout : JBPanel<JBPanel<*>>() {
        init {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
        }
    }
}
