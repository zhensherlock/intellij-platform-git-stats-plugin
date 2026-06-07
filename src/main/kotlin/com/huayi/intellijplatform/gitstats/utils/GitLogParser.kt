package com.huayi.intellijplatform.gitstats.utils

internal object GitLogParser {
    private const val DETAILED_SEPARATOR = "\u001F"

    fun parseFastSummary(output: String): Array<UserStats> {
        val userStatsData = mutableMapOf<String, UserStats>()
        var currentUserStatsData: UserStats? = null
        output.lineSequence().forEach { line ->
            if (line.isEmpty()) return@forEach
            val commitFilesStatsData = parseNumstatLine(line)
            if (commitFilesStatsData == null) {
                currentUserStatsData = userStatsData.getOrPut(line) { UserStats(line) }
                return@forEach
            }
            currentUserStatsData?.apply {
                addedLines += commitFilesStatsData.addedLines
                deletedLines += commitFilesStatsData.deletedLines
                modifiedFileCount++
            }
        }
        return userStatsData.values
            .sortedByDescending { it.addedLines }
            .toTypedArray()
    }

    fun parseDetailed(output: String): Array<UserStats> {
        val userStatsData = mutableMapOf<String, UserStats>()
        var currentUserStatsData: UserStats? = null
        output.lineSequence().forEach { line ->
            if (line.startsWith(DETAILED_SEPARATOR)) {
                val parts = line.split(DETAILED_SEPARATOR)
                if (parts.size >= 4) {
                    val hash = parts[1]
                    val date = parts[2]
                    val author = parts.drop(3).joinToString(DETAILED_SEPARATOR)
                    currentUserStatsData = userStatsData.getOrPut(author) { UserStats(author) }.apply {
                        commitCount++
                        commits.add(CommitStats(hash, date))
                    }
                }
                return@forEach
            }
            if (line.isNotEmpty()) {
                val commitFilesStatsData = parseNumstatLine(line) ?: return@forEach
                currentUserStatsData?.let { userStats ->
                    userStats.apply {
                        addedLines += commitFilesStatsData.addedLines
                        deletedLines += commitFilesStatsData.deletedLines
                        modifiedFileCount++
                    }
                    userStats.commits.lastOrNull()?.let { commitStats ->
                        commitStats.apply {
                            files.add(commitFilesStatsData)
                            addedLines += commitFilesStatsData.addedLines
                            deletedLines += commitFilesStatsData.deletedLines
                            modifiedFileCount++
                        }
                    }
                }
            }
        }
        return userStatsData.values.toTypedArray()
    }

    internal fun parseNumstatLine(line: String): CommitFilesStats? {
        val parts = line.split("\t", limit = 3)
        if (parts.size < 3) return null
        return CommitFilesStats(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0, parts[2])
    }
}
