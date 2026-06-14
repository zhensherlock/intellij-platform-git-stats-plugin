package com.huayi.intellijplatform.gitstats.utils

import git4idea.config.GitExecutableManager
import java.util.concurrent.TimeUnit
import com.intellij.openapi.project.Project
import com.huayi.intellijplatform.gitstats.models.BranchInfo
import com.huayi.intellijplatform.gitstats.models.BranchScope
import com.huayi.intellijplatform.gitstats.models.SettingModel

class GitUtils(
    project: Project,
    private val commandRunner: CommandRunner = ProcessCommandRunner
) {
    private val gitExecutablePath: String = GitExecutableManager.getInstance().getExecutable(project).exePath
    private val basePath: String = project.basePath as String
    private val commandBuilder = GitLogCommandBuilder(gitExecutablePath)

    fun checkRepository(): GitDataResult<Unit> {
        val commandResult = commandRunner.run(
            basePath,
            commandBuilder.checkRepositoryCommand(),
            GIT_REPOSITORY_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        commandFailure(commandResult)?.let { return it }
        return if (commandResult.output.trim() == "true") {
            GitDataResult.Success(Unit)
        } else {
            GitDataResult.Failure(GitFailureReason.NOT_GIT_REPOSITORY, commandResult.details())
        }
    }

    fun getBranchInfo(): GitDataResult<BranchInfo> {
        val currentBranchResult = commandRunner.run(
            basePath,
            commandBuilder.currentBranchCommand(),
            GIT_REPOSITORY_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        commandFailure(currentBranchResult)?.let { return it }

        val localBranchesResult = commandRunner.run(
            basePath,
            commandBuilder.localBranchesCommand(),
            GIT_REPOSITORY_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        commandFailure(localBranchesResult)?.let { return it }

        val remoteBranchesResult = commandRunner.run(
            basePath,
            commandBuilder.remoteBranchesCommand(),
            GIT_REPOSITORY_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        commandFailure(remoteBranchesResult)?.let { return it }

        return GitDataResult.Success(
            BranchInfo(
                currentBranch = currentBranchResult.output.trim().ifEmpty { null },
                localBranches = parseBranchLines(localBranchesResult.output),
                remoteBranches = parseBranchLines(remoteBranchesResult.output)
                    .filterNot { it.endsWith("/HEAD") }
            )
        )
    }

    fun getTopSpeedUserStats(
        startDate: String?,
        endDate: String?,
        settingModel: SettingModel,
        branchScope: BranchScope = BranchScope.CurrentBranch
    ): GitDataResult<Array<UserStats>> {
        val command = commandBuilder.fastSummaryCommand(startDate, endDate, settingModel.excludePaths(), branchScope)
        val commandResult = commandRunner.run(basePath, command, GIT_LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (commandResult.isEmptyRepositoryLog()) {
            return GitDataResult.Success(emptyArray())
        }
        commandFailure(commandResult)?.let { return it }
        return GitDataResult.Success(GitLogParser.parseFastSummary(commandResult.output))
    }

    fun getUserStats(
        startDate: String?,
        endDate: String?,
        settingModel: SettingModel,
        branchScope: BranchScope = BranchScope.CurrentBranch
    ): GitDataResult<Array<UserStats>> {
        val command = commandBuilder.detailedCommand(startDate, endDate, settingModel.excludePaths(), branchScope)
        val commandResult = commandRunner.run(basePath, command, GIT_LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (commandResult.isEmptyRepositoryLog()) {
            return GitDataResult.Success(emptyArray())
        }
        commandFailure(commandResult)?.let { return it }
        return GitDataResult.Success(GitLogParser.parseDetailed(commandResult.output))
    }

    private fun commandFailure(commandResult: CommandResult): GitDataResult.Failure? {
        return when {
            commandResult.timedOut -> GitDataResult.Failure(GitFailureReason.TIMEOUT, commandResult.details())
            commandResult.exceptionMessage != null -> GitDataResult.Failure(
                GitFailureReason.GIT_UNAVAILABLE,
                commandResult.details()
            )

            commandResult.exitCode != 0 -> {
                val details = commandResult.details()
                val reason = if (details.contains("not a git repository", ignoreCase = true)) {
                    GitFailureReason.NOT_GIT_REPOSITORY
                } else {
                    GitFailureReason.COMMAND_FAILED
                }
                GitDataResult.Failure(reason, details)
            }

            else -> null
        }
    }

    private fun CommandResult.isEmptyRepositoryLog(): Boolean {
        return exitCode == 128 &&
            output.contains("does not have any commits yet", ignoreCase = true)
    }

    private fun parseBranchLines(output: String): List<String> {
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .toList()
    }

    private companion object {
        private const val GIT_REPOSITORY_TIMEOUT_SECONDS = 10L
        private const val GIT_LOG_TIMEOUT_SECONDS = 60L
    }
}
