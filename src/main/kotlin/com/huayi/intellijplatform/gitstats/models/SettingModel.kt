package com.huayi.intellijplatform.gitstats.models

data class SettingModel(
    var mode: String = MODE_FAST_SUMMARY,
    var exclude: String = ""
) {
    fun excludePaths(): List<String> = parseExcludePaths(exclude)

    companion object {
        const val MODE_FAST_SUMMARY = "Fast Summary"
        const val MODE_DETAILED = "Detailed"

        fun parseExcludePaths(exclude: String): List<String> = exclude
            .lineSequence()
            .map { it.trim().replace('\\', '/') }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }
}
