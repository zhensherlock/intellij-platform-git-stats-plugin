package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.utils.DateRanges
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.util.Date
import javax.swing.JComponent

class DateRangeFilterAction(
    initialStartDate: Date?,
    initialEndDate: Date?,
    private val onRangeChanged: (Date?, Date?) -> Unit
) : DumbAwareAction(), CustomComponentAction {
    private val components = mutableSetOf<GitLogFilterChip>()
    private var currentStartDate = initialStartDate
    private var currentEndDate = initialEndDate
    private var currentPreset: DateRangePreset? = null
    private var currentPopup: JBPopup? = null

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.text = toolbarText()
        e.presentation.description = dateRangeTooltip()
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return GitLogFilterChip(
            onOpenPopup = ::showPopup,
            onClear = ::resetToAll
        ).also {
            components.add(it)
            it.update(createChipModel())
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val component = e.inputEvent?.component as? JComponent ?: components.firstOrNull() ?: return
        showPopup(component)
    }

    private fun showPopup(component: JComponent) {
        currentPopup?.cancel()
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            DateRangePopupActionGroupFactory(
                applyPreset = ::setPreset,
                requestCustomRange = { showCustomRangePopup(component) }
            ).create(),
            DataManager.getInstance().getDataContext(component),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
            {
                currentPopup = null
            },
            -1
        )
        currentPopup = popup
        popup.showUnderneathOf(component)
    }

    private fun showCustomRangePopup(component: JComponent) {
        closePopup()
        currentPopup = DateRangePopup.show(
            anchor = component,
            startDate = currentStartDate,
            endDate = currentEndDate,
            applyRange = ::setCustomRange
        ) {
            currentPopup = null
        }
    }

    private fun setCustomRange(start: Date?, end: Date?) {
        currentPreset = null
        setRange(start, end)
    }

    private fun setPreset(preset: DateRangePreset) {
        currentPreset = preset
        val (start, end) = preset.range()
        setRange(start, end)
        closePopup()
    }

    private fun setRange(start: Date?, end: Date?) {
        if (start == null || end == null) {
            currentStartDate = null
            currentEndDate = null
            refreshComponents()
            onRangeChanged(null, null)
            return
        }

        val (normalizedStart, normalizedEnd) = DateRanges.orderedRange(
            DateRanges.startOfDay(start),
            DateRanges.endOfDay(end)
        )
        currentStartDate = normalizedStart
        currentEndDate = normalizedEnd
        refreshComponents()
        onRangeChanged(normalizedStart, normalizedEnd)
    }

    private fun resetToAll() {
        currentPreset = null
        setRange(null, null)
    }

    private fun refreshComponents() {
        components.removeIf { it.parent == null }
        components.forEach { it.update(createChipModel()) }
    }

    private fun createChipModel(): GitLogFilterChip.Model {
        val dateRangeText = chipValueText()
        return GitLogFilterChip.Model(
            label = MyBundle.message("filterDateButtonLabel"),
            value = dateRangeText,
            tooltip = dateRangeTooltip(),
            clearTooltip = MyBundle.message("filterDateClearTooltip")
        )
    }

    private fun toolbarText(): String {
        val dateRangeText = chipValueText()
        return if (dateRangeText == null) {
            MyBundle.message("filterDateButtonLabel")
        } else {
            "${MyBundle.message("filterDateButtonLabel")}: $dateRangeText"
        }
    }

    private fun dateRangeTooltip(): String {
        return dateRangeText()?.let { MyBundle.message("filterDateTooltip", it) }
            ?: MyBundle.message("filterDateEmptyTooltip")
    }

    private fun chipValueText(): String? {
        return currentPreset?.label() ?: dateRangeText()
    }

    private fun dateRangeText(): String? {
        val start = currentStartDate
        val end = currentEndDate
        return if (start == null || end == null) {
            null
        } else {
            "${DateRanges.formatDate(start)} - ${DateRanges.formatDate(end)}"
        }
    }

    private fun closePopup() {
        currentPopup?.closeOk(null)
        currentPopup = null
    }
}
