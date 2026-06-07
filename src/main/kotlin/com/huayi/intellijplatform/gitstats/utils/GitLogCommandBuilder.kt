package com.huayi.intellijplatform.gitstats.utils

internal class GitLogCommandBuilder(private val gitExecutablePath: String) {
    fun checkRepositoryCommand(): List<String> {
        return listOf(gitExecutablePath, "rev-parse", "--is-inside-work-tree")
    }

    fun fastSummaryCommand(startDate: String, endDate: String, excludePaths: List<String>): List<String> {
        return buildLogCommand(
            listOf(
                "--format=%aN",
                "--since=$startDate",
                "--until=$endDate",
                "--numstat"
            ),
            excludePaths
        )
    }

    fun detailedCommand(startDate: String, endDate: String, excludePaths: List<String>): List<String> {
        return buildLogCommand(
            listOf(
                "--numstat",
                "--pretty=format:%x1f%h%x1f%ad%x1f%aN",
                "--since=$startDate",
                "--until=$endDate"
            ),
            excludePaths
        )
    }

    private fun buildLogCommand(options: List<String>, excludePaths: List<String>): List<String> {
        return listOf(gitExecutablePath, "log") + options + listOf("--") + buildPathspecArgs(excludePaths)
    }

    internal fun buildPathspecArgs(excludePaths: List<String>): List<String> {
        return listOf(".") + excludePaths.map { ":(exclude)$it" }
    }
}
