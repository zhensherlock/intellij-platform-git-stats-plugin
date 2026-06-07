package com.huayi.intellijplatform.gitstats.models

enum class StatsMode(
    val id: String,
    val legacyLabel: String,
    val labelKey: String
) {
    FAST_SUMMARY("fast_summary", "Fast Summary", "settingsModeFastSummary"),
    DETAILED("detailed", "Detailed", "settingsModeDetailed");

    companion object {
        fun fromStoredValue(value: String?): StatsMode {
            return entries.firstOrNull { mode ->
                value == mode.id || value == mode.legacyLabel
            } ?: FAST_SUMMARY
        }
    }
}
