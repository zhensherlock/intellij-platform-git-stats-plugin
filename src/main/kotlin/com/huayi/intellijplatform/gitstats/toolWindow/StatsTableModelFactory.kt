package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.StatsMode
import com.huayi.intellijplatform.gitstats.services.GitStatsReport
import com.huayi.intellijplatform.gitstats.utils.UserStats

internal object StatsTableModelFactory {
    fun fromReport(report: GitStatsReport): StatsTableModel {
        return fromUserStats(report.userStats, report.mode)
    }

    fun fromUserStats(userStats: List<UserStats>, mode: StatsMode): StatsTableModel {
        val rows = userStats.map { item ->
            StatsTableRow(
                author = item.author,
                commitCount = if (mode == StatsMode.DETAILED) item.commitCount else null,
                addedLines = item.addedLines,
                deletedLines = item.deletedLines,
                modifiedFileCount = item.modifiedFileCount
            )
        }
        return StatsTableModel(rows, columnsFor(mode))
    }

    private fun columnsFor(mode: StatsMode): List<StatsTableColumn> {
        return buildList {
            add(stringColumn(StatsTableColumnKind.AUTHOR, "statsTableColumnAuthor"))
            if (mode == StatsMode.DETAILED) {
                add(intColumn(StatsTableColumnKind.COMMITS, "statsTableColumnCommits"))
            }
            add(intColumn(StatsTableColumnKind.LINES_ADDED, "statsTableColumnLinesAdded"))
            add(intColumn(StatsTableColumnKind.LINES_DELETED, "statsTableColumnLinesDeleted"))
            add(intColumn(StatsTableColumnKind.MODIFIED_FILES, "statsTableColumnModifiedFiles"))
        }
    }

    private fun stringColumn(kind: StatsTableColumnKind, messageKey: String) =
        StatsTableColumn(kind, MyBundle.message(messageKey), String::class.java)

    private fun intColumn(kind: StatsTableColumnKind, messageKey: String) =
        StatsTableColumn(kind, MyBundle.message(messageKey), Int::class.javaObjectType)
}
