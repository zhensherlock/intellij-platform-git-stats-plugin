package com.huayi.intellijplatform.gitstats.utils

import git4idea.config.GitExecutableManager
import java.util.concurrent.TimeUnit
import com.intellij.openapi.project.Project
import com.huayi.intellijplatform.gitstats.models.SettingModel

data class UserStats(
    val author: String,
    var addedLines: Int = 0,
    var deletedLines: Int = 0,
    var modifiedFileCount: Int = 0,
    var commitCount: Int = 0,
    var commits: MutableList<CommitStats> = mutableListOf()
)

data class CommitStats(
    val hash: String,
    val date: String,
    var addedLines: Int = 0,
    var deletedLines: Int = 0,
    var modifiedFileCount: Int = 0,
    var files: MutableList<CommitFilesStats> = mutableListOf()
)

data class CommitFilesStats(
    var addedLines: Int = 0, var deletedLines: Int = 0, var fileName: String
)

enum class GitFailureReason {
    NOT_GIT_REPOSITORY,
    GIT_UNAVAILABLE,
    COMMAND_FAILED,
    TIMEOUT
}

sealed class GitDataResult<out T> {
    data class Success<T>(val data: T) : GitDataResult<T>()
    data class Failure(val reason: GitFailureReason, val details: String? = null) : GitDataResult<Nothing>()
}

class GitUtils(project: Project) {
    private val gitExecutablePath: String = GitExecutableManager.getInstance().getExecutable(project).exePath
    private val basePath: String = project.basePath as String

    fun checkRepository(): GitDataResult<Unit> {
        val commandResult = Utils.runCommand(
            basePath,
            listOf(gitExecutablePath, "rev-parse", "--is-inside-work-tree"),
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
        val timeoutAmount = 60L
        val timeUnit = TimeUnit.SECONDS
        val command = buildGitLogCommand(
            listOf(
                "--format=%aN",
                "--since=$startDate",
                "--until=$endDate",
                "--numstat"
            ),
            settingModel
        )
        val commandResult = Utils.runCommand(basePath, command, timeoutAmount, timeUnit)
        if (commandResult.isEmptyRepositoryLog()) {
            return GitDataResult.Success(emptyArray())
        }
        commandFailure(commandResult)?.let { return it }

        val userStatsData = mutableMapOf<String, UserStats>()
        var currentUserStatsData: UserStats? = null
        commandResult.output.lineSequence().forEach { line ->
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
        return GitDataResult.Success(
            userStatsData.values
                .sortedByDescending { it.addedLines }
                .toTypedArray()
        )
    }

    fun getUserStats(
        startDate: String,
        endDate: String,
        settingModel: SettingModel
    ): GitDataResult<Array<UserStats>> {
        val timeoutAmount = 60L
        val timeUnit = TimeUnit.SECONDS
        val separator = "\u001F"
        val command = buildGitLogCommand(
            listOf(
                "--numstat",
                "--pretty=format:%x1f%h%x1f%ad%x1f%aN",
                "--since=$startDate",
                "--until=$endDate"
            ),
            settingModel
        )
        val commandResult = Utils.runCommand(basePath, command, timeoutAmount, timeUnit)
        if (commandResult.isEmptyRepositoryLog()) {
            return GitDataResult.Success(emptyArray())
        }
        commandFailure(commandResult)?.let { return it }

        val userStatsData = mutableMapOf<String, UserStats>()
        var currentUserStatsData: UserStats? = null
        commandResult.output.lineSequence().forEach { line ->
            if (line.startsWith(separator)) {
                val parts = line.split(separator)
                if (parts.size >= 4) {
                    val hash = parts[1]
                    val date = parts[2]
                    val author = parts.drop(3).joinToString(separator)
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
        return GitDataResult.Success(userStatsData.values.toTypedArray())
    }

    private fun buildGitLogCommand(options: List<String>, settingModel: SettingModel): List<String> {
        return listOf(gitExecutablePath, "log") + options + listOf("--") + buildPathspecArgs(settingModel)
    }

    private fun buildPathspecArgs(settingModel: SettingModel): List<String> {
        return listOf(".") + settingModel.excludePaths().map { ":(exclude)$it" }
    }

    private fun parseNumstatLine(line: String): CommitFilesStats? {
        val parts = line.split("\t", limit = 3)
        if (parts.size < 3) return null
        return CommitFilesStats(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0, parts[2])
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
}
