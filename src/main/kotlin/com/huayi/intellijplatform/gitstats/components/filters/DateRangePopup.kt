package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.utils.DateRanges
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.CalendarView
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.util.Date
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent

internal object DateRangePopup {
    fun show(
        anchor: JComponent,
        startDate: Date?,
        endDate: Date?,
        applyRange: (Date?, Date?) -> Unit,
        onClosed: () -> Unit
    ): JBPopup {
        val (fallbackStartDate, fallbackEndDate) = DateRanges.lastSevenDaysDateRange()
        val startCalendarView = CalendarView(CalendarView.Mode.DATE).apply {
            setDate(DateRanges.startOfDay(startDate ?: fallbackStartDate))
        }
        val endCalendarView = CalendarView(CalendarView.Mode.DATE).apply {
            setDate(DateRanges.endOfDay(endDate ?: fallbackEndDate))
        }
        var popup: JBPopup? = null

        fun commitRange(start: Date, end: Date) {
            val (orderedStart, orderedEnd) = DateRanges.orderedRange(
                DateRanges.startOfDay(start),
                DateRanges.endOfDay(end)
            )
            applyRange(orderedStart, orderedEnd)
            popup?.closeOk(null)
        }

        val applyButton = JButton(MyBundle.message("datePickerApplyButton")).apply {
            addActionListener {
                commitRange(startCalendarView.date, endCalendarView.date)
            }
        }
        startCalendarView.registerEnterHandler {
            applyButton.doClick()
        }
        endCalendarView.registerEnterHandler {
            applyButton.doClick()
        }

        val calendarsPanel = HorizontalPanel().apply {
            add(startCalendarView)
            add(JBBox.createHorizontalStrut(12))
            add(endCalendarView)
        }
        val actionsPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(8)
            add(applyButton, BorderLayout.EAST)
        }
        val popupContent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(calendarsPanel, BorderLayout.CENTER)
            add(actionsPanel, BorderLayout.SOUTH)
        }

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(popupContent, startCalendarView.daysCombo)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .createPopup()
        popup.setFinalRunnable(onClosed)
        popup.showUnderneathOf(anchor)
        return popup
    }

    private class HorizontalPanel : JBPanel<JBPanel<*>>() {
        init {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.X_AXIS)
        }
    }
}
