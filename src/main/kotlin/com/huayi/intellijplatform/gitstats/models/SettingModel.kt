package com.huayi.intellijplatform.gitstats.models

data class SettingModel(
    var mode: String = MODE_FAST_SUMMARY,
    var exclude: String = ""
) {
    companion object {
        const val MODE_FAST_SUMMARY = "Fast Summary"
        const val MODE_DETAILED = "Detailed"
    }
}
