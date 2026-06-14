package com.huayi.intellijplatform.gitstats.components.filters

internal object PathFilterPaths {
    fun parse(text: String): List<String> {
        return normalize(text.lineSequence().asIterable())
    }

    fun normalize(paths: Iterable<String>): List<String> {
        return paths
            .mapNotNull { normalizePath(it) }
            .distinct()
            .toList()
    }

    private fun normalizePath(path: String): String? {
        val normalized = path.trim().replace('\\', '/')
        if (normalized.isEmpty() || normalized.startsWith(":") || normalized.startsWith("/")) {
            return null
        }
        if (WINDOWS_DRIVE_PATH_PATTERN.matches(normalized)) {
            return null
        }

        val segments = normalized.split('/')
        if (segments.any { it == ".." }) {
            return null
        }

        return segments
            .filter { it.isNotEmpty() && it != "." }
            .joinToString("/")
            .ifEmpty { "." }
    }

    private val WINDOWS_DRIVE_PATH_PATTERN = Regex("""^[A-Za-z]:.*""")
}
