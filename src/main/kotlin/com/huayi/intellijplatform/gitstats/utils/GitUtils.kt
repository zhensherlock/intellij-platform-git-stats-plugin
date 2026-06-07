package com.huayi.intellijplatform.gitstats.utils

import git4idea.config.GitExecutableManager
import java.util.concurrent.TimeUnit
import com.intellij.openapi.project.Project
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
            10L,
            TimeUnit.SECONDS
        )
        commandFailure(commandResult)?.let { return it }
        return if (commandResult.output.trim() == "true") {
            GitDataResult.Success(Unit)
        } else {
            GitDataResult.Failure(GitFailureReason.NOT_GIT_REPOSITORY, commandResult.details())
        }
    }

    fun getTopSpeedUserStats(
        startDate: String, endDate: String, settingModel: SettingModel
    ): GitDataResult<Array<UserStats>> {
        val command = commandBuilder.fastSummaryCommand(startDate, endDate, settingModel.excludePaths())
        val commandResult = commandRunner.run(basePath, command, GIT_LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (commandResult.isEmptyRepositoryLog()) {
            return GitDataResult.Success(emptyArray())
        }
        commandFailure(commandResult)?.let { return it }
        return GitDataResult.Success(GitLogParser.parseFastSummary(commandResult.output))
    }

    fun getUserStats(
        startDate: String,
        endDate: String,
        settingModel: SettingModel
    ): GitDataResult<Array<UserStats>> {
        val command = commandBuilder.detailedCommand(startDate, endDate, settingModel.excludePaths())
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

    private companion object {
        private const val GIT_LOG_TIMEOUT_SECONDS = 60L
    }
}
