package com.huayi.intellijplatform.gitstats.services

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.huayi.intellijplatform.gitstats.toolWindow.StatsTableModel
import com.huayi.intellijplatform.gitstats.utils.GitDataResult
import com.huayi.intellijplatform.gitstats.utils.GitFailureReason
import com.huayi.intellijplatform.gitstats.utils.GitUtils
import com.huayi.intellijplatform.gitstats.utils.UserStats
import com.huayi.intellijplatform.gitstats.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

sealed class GitStatsResult {
    data class Success(val model: StatsTableModel) : GitStatsResult()
    data class Empty(val title: String, val message: String) : GitStatsResult()
    data class Failure(
        val title: String,
        val message: String,
        val details: String? = null,
        val notify: Boolean = false
    ) : GitStatsResult()
}

@Service(Service.Level.PROJECT)
class GitStatsService(p: Project) {
    private var project: Project

    init {
        project = p
    }

    fun getUserStats(startTime: Date, endTime: Date, settingModel: SettingModel): GitStatsResult {
        return collectStats(startTime, endTime, settingModel) { gitUtils, startDate, endDate ->
            gitUtils.getUserStats(startDate, endDate, settingModel)
        }
    }

    fun getTopSpeedUserStats(startTime: Date, endTime: Date, settingModel: SettingModel): GitStatsResult {
        return collectStats(startTime, endTime, settingModel) { gitUtils, startDate, endDate ->
            gitUtils.getTopSpeedUserStats(startDate, endDate, settingModel)
        }
    }

    private fun collectStats(
        startTime: Date,
        endTime: Date,
        settingModel: SettingModel,
        loadStats: (GitUtils, String, String) -> GitDataResult<Array<UserStats>>
    ): GitStatsResult {
        if (!Utils.checkDirectoryExists(project.basePath)) {
            return GitStatsResult.Failure(
                MyBundle.message("stateInvalidProjectTitle"),
                MyBundle.message("stateInvalidProjectMessage")
            )
        }
        val gitUtils = runCatching { GitUtils(project) }.getOrElse {
            return GitStatsResult.Failure(
                MyBundle.message("stateGitUnavailableTitle"),
                MyBundle.message("stateGitUnavailableMessage"),
                it.stackTraceToString(),
                notify = true
            )
        }

        when (val repositoryResult = gitUtils.checkRepository()) {
            is GitDataResult.Failure -> return repositoryResult.toGitStatsResult()
            is GitDataResult.Success -> Unit
        }

        val startDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime)
        val endDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime)
        return when (val statsResult = loadStats(gitUtils, startDate, endDate)) {
            is GitDataResult.Failure -> statsResult.toGitStatsResult()
            is GitDataResult.Success -> {
                if (statsResult.data.isEmpty()) {
                    GitStatsResult.Empty(
                        MyBundle.message("stateNoDataTitle"),
                        MyBundle.message("stateNoDataMessage")
                    )
                } else {
                    GitStatsResult.Success(createTableModel(statsResult.data, settingModel))
                }
            }
        }
    }

    private fun createTableModel(
        userStats: Array<UserStats>,
        settingModel: SettingModel
    ): StatsTableModel {
        if (settingModel.mode == SettingModel.MODE_DETAILED) {
            val detailedData = userStats.map { item ->
                arrayOf(
                    item.author,
                    item.commitCount.toString(),
                    item.addedLines.toString(),
                    item.deletedLines.toString(),
                    item.modifiedFileCount.toString()
                )
            }.toTypedArray()
            return StatsTableModel(
                detailedData,
                arrayOf(
                    MyBundle.message("statsTableColumnAuthor"),
                    MyBundle.message("statsTableColumnCommits"),
                    MyBundle.message("statsTableColumnLinesAdded"),
                    MyBundle.message("statsTableColumnLinesDeleted"),
                    MyBundle.message("statsTableColumnModifiedFiles")
                )
            )
        }
        val data = userStats.map { item ->
            arrayOf(
                item.author,
                item.addedLines.toString(),
                item.deletedLines.toString(),
                item.modifiedFileCount.toString()
            )
        }.toTypedArray()
        return StatsTableModel(
            data,
            arrayOf(
                MyBundle.message("statsTableColumnAuthor"),
                MyBundle.message("statsTableColumnLinesAdded"),
                MyBundle.message("statsTableColumnLinesDeleted"),
                MyBundle.message("statsTableColumnModifiedFiles")
            )
        )
    }

    private fun GitDataResult.Failure.toGitStatsResult(): GitStatsResult.Failure {
        val summary = summarize(details)
        if (reason != GitFailureReason.NOT_GIT_REPOSITORY) {
            thisLogger().warn("Git stats failed: $reason\n${details.orEmpty()}")
        }
        return when (reason) {
            GitFailureReason.NOT_GIT_REPOSITORY -> GitStatsResult.Failure(
                MyBundle.message("stateNotGitRepositoryTitle"),
                MyBundle.message("stateNotGitRepositoryMessage"),
                details
            )

            GitFailureReason.GIT_UNAVAILABLE -> GitStatsResult.Failure(
                MyBundle.message("stateGitUnavailableTitle"),
                MyBundle.message("stateGitUnavailableMessage"),
                details,
                notify = true
            )

            GitFailureReason.TIMEOUT -> GitStatsResult.Failure(
                MyBundle.message("stateGitTimeoutTitle"),
                MyBundle.message("stateGitTimeoutMessage"),
                details
            )

            GitFailureReason.COMMAND_FAILED -> GitStatsResult.Failure(
                MyBundle.message("stateGitFailedTitle"),
                MyBundle.message("stateGitFailedMessage", summary),
                details
            )
        }
    }

    private fun summarize(details: String?): String {
        return details
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() && !it.startsWith("Exit code:") }
            ?.take(240)
            ?: MyBundle.message("stateUnknownGitError")
    }
}
