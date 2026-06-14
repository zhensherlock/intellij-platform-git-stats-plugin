package com.huayi.intellijplatform.gitstats.services

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.BranchInfo
import com.huayi.intellijplatform.gitstats.models.BranchScope
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.huayi.intellijplatform.gitstats.models.StatsMode
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.huayi.intellijplatform.gitstats.utils.GitDataResult
import com.huayi.intellijplatform.gitstats.utils.GitFailureReason
import com.huayi.intellijplatform.gitstats.utils.GitUtils
import com.huayi.intellijplatform.gitstats.utils.UserStats
import com.huayi.intellijplatform.gitstats.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

data class GitStatsReport(
    val mode: StatsMode,
    val userStats: List<UserStats>
)

sealed class GitStatsResult {
    data class Success(val report: GitStatsReport) : GitStatsResult()
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

    fun getStats(
        startTime: Date?,
        endTime: Date?,
        settingModel: SettingModel,
        branchScope: BranchScope = BranchScope.CurrentBranch,
        includePaths: List<String> = emptyList()
    ): GitStatsResult {
        return when (settingModel.statsMode()) {
            StatsMode.FAST_SUMMARY -> getTopSpeedUserStats(startTime, endTime, settingModel, branchScope, includePaths)
            StatsMode.DETAILED -> getUserStats(startTime, endTime, settingModel, branchScope, includePaths)
        }
    }

    fun getBranchInfo(): GitDataResult<BranchInfo> {
        if (!Utils.checkDirectoryExists(project.basePath)) {
            return GitDataResult.Failure(GitFailureReason.NOT_GIT_REPOSITORY)
        }
        val gitUtils = runCatching { GitUtils(project) }.getOrElse {
            return GitDataResult.Failure(GitFailureReason.GIT_UNAVAILABLE, it.stackTraceToString())
        }
        return when (val repositoryResult = gitUtils.checkRepository()) {
            is GitDataResult.Failure -> repositoryResult
            is GitDataResult.Success -> gitUtils.getBranchInfo()
        }
    }

    fun getUserStats(
        startTime: Date?,
        endTime: Date?,
        settingModel: SettingModel,
        branchScope: BranchScope = BranchScope.CurrentBranch,
        includePaths: List<String> = emptyList()
    ): GitStatsResult {
        return collectStats(startTime, endTime, settingModel, StatsMode.DETAILED) { gitUtils, startDate, endDate ->
            gitUtils.getUserStats(startDate, endDate, settingModel, branchScope, includePaths)
        }
    }

    fun getTopSpeedUserStats(
        startTime: Date?,
        endTime: Date?,
        settingModel: SettingModel,
        branchScope: BranchScope = BranchScope.CurrentBranch,
        includePaths: List<String> = emptyList()
    ): GitStatsResult {
        return collectStats(startTime, endTime, settingModel, StatsMode.FAST_SUMMARY) { gitUtils, startDate, endDate ->
            gitUtils.getTopSpeedUserStats(startDate, endDate, settingModel, branchScope, includePaths)
        }
    }

    private fun collectStats(
        startTime: Date?,
        endTime: Date?,
        settingModel: SettingModel,
        mode: StatsMode,
        loadStats: (GitUtils, String?, String?) -> GitDataResult<Array<UserStats>>
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

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val startDate = startTime?.let { formatter.format(it) }
        val endDate = endTime?.let { formatter.format(it) }
        return when (val statsResult = loadStats(gitUtils, startDate, endDate)) {
            is GitDataResult.Failure -> statsResult.toGitStatsResult()
            is GitDataResult.Success -> {
                if (statsResult.data.isEmpty()) {
                    GitStatsResult.Empty(
                        MyBundle.message("stateNoDataTitle"),
                        MyBundle.message("stateNoDataMessage")
                    )
                } else {
                    GitStatsResult.Success(GitStatsReport(mode, statsResult.data.toList()))
                }
            }
        }
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
