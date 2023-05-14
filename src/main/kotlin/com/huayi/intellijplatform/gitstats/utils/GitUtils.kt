package com.huayi.intellijplatform.gitstats.utils

import java.util.concurrent.TimeUnit

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

class GitUtil(private val repoPath: String) {

    fun getTopSpeedUserStats(
        startDate: String, endDate: String, timeoutAmount: Long = 60L, timeUnit: TimeUnit = TimeUnit.SECONDS
    ) {
        val os = Utils.getOS()
        val command = listOf(
            if (os == "Windows") "cmd" else "/bin/sh",
            if (os == "Windows") "/c" else "-c",
            "git log --format=\"%aN\" | sort -u | while read name; do echo \"\$name\\t\"; git log --author=\"\$name\" --pretty=tformat: --since==\"$startDate\" --until=\"$endDate\" --numstat | awk '{ add += \$1; subs += \$2; loc += \$1 - \$2; file++ } END { printf \"added lines: %s, removed lines: %s, total lines: %s, modified files: %s\\n\", add, subs, loc, file }' -; done"
        )
        val process = Utils.runCommand(repoPath, command, timeoutAmount, timeUnit)
//        val userStatsData = mutableMapOf<String, UserStats>()
        process!!.inputStream.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                println(line)
            }
        }
    }

    fun getUserStats(
        startDate: String,
        endDate: String,
        timeoutAmount: Long = 60L,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        separator: String = "--"
    ): Array<UserStats> {
        val gitCommand = listOf(
            "git",
            "log",
            "--numstat",
            "--date=iso",
            "--pretty=format:${separator}%h${separator}%ad${separator}%aN",
            "--since=$startDate",
            "--until=$endDate"
        )

        val process = Utils.runCommand(repoPath, gitCommand, timeoutAmount, timeUnit)

//        val processBuilder = ProcessBuilder(gitCommand)
//            .directory(File(repoPath))
//            .redirectErrorStream(true)
//            .redirectOutput(ProcessBuilder.Redirect.PIPE)
//        val process = processBuilder.start().also { it.waitFor(timeoutAmount, timeUnit) }

        val userStatsData = mutableMapOf<String, UserStats>()
        process!!.inputStream.bufferedReader().use { reader ->
            var currentUserStatsData: UserStats? = null
            reader.forEachLine { line ->
                if (line.startsWith(separator)) {
                    val (_, hash, date, author) = line.split(separator)
                    currentUserStatsData = userStatsData.getOrPut(author) { UserStats(author) }.apply {
                        commitCount++
                        commits.add(CommitStats(hash, date))
                    }
                    return@forEachLine
                }
                if (line.isNotEmpty()) {
                    val commitFilesStatsData = line.split("\t").let {
                        CommitFilesStats(it[0].toIntOrNull() ?: 0, it[1].toIntOrNull() ?: 0, it[2])
                    }
                    currentUserStatsData!!.let { userStats ->
                        userStats.apply {
                            addedLines += commitFilesStatsData.addedLines
                            deletedLines += commitFilesStatsData.deletedLines
                            modifiedFileCount++
                        }
                        userStats.commits.last().let { commitStats ->
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
        }
        return userStatsData.values.toTypedArray()
    }
}