package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.utils.DateRanges
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import java.util.Date

internal enum class DateRangePreset(
    private val labelKey: String
) {
    THIS_WEEK("datePickerThisWeekButton"),
    LAST_SEVEN_DAYS("datePickerLast7DaysButton"),
    THIS_MONTH("datePickerThisMonthButton");

    fun label(): String = MyBundle.message(labelKey)

    fun range(): Pair<Date, Date> {
        return when (this) {
            THIS_WEEK -> DateRanges.thisWeekDateTimeRange()
            LAST_SEVEN_DAYS -> DateRanges.lastSevenDaysDateRange()
            THIS_MONTH -> DateRanges.thisMonthDateRange()
        }
    }
}

internal class DateRangePopupActionGroupFactory(
    private val applyPreset: (DateRangePreset) -> Unit,
    private val requestCustomRange: () -> Unit
) {
    fun create(): DefaultActionGroup {
        return DefaultActionGroup().apply {
            add(SelectDateRangeAction())
            addSeparator()
            DateRangePreset.entries.forEach { preset ->
                add(DateRangePresetAction(preset))
            }
        }
    }

    private inner class SelectDateRangeAction : DumbAwareAction(MyBundle.message("datePickerSelectButton")) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            requestCustomRange()
        }
    }

    private inner class DateRangePresetAction(
        private val preset: DateRangePreset
    ) : DumbAwareAction(preset.label()) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            applyPreset(preset)
        }
    }
}
