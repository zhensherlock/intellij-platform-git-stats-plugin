package com.huayi.intellijplatform.gitstats.utils

import com.huayi.intellijplatform.gitstats.models.BranchScope

internal class GitLogCommandBuilder(private val gitExecutablePath: String) {
    fun checkRepositoryCommand(): List<String> {
        return listOf(gitExecutablePath, "rev-parse", "--is-inside-work-tree")
    }

    fun currentBranchCommand(): List<String> {
        return listOf(gitExecutablePath, "branch", "--show-current")
    }

    fun localBranchesCommand(): List<String> {
        return listOf(gitExecutablePath, "for-each-ref", "--format=%(refname:short)", "refs/heads")
    }

    fun remoteBranchesCommand(): List<String> {
        return listOf(gitExecutablePath, "for-each-ref", "--format=%(refname:short)", "refs/remotes")
    }

    fun fastSummaryCommand(
        startDate: String?,
        endDate: String?,
        excludePaths: List<String>,
        branchScope: BranchScope = BranchScope.CurrentBranch,
        includePaths: List<String> = emptyList()
    ): List<String> {
        return buildLogCommand(
            listOf("--format=%aN") + buildDateArgs(startDate, endDate) + listOf("--numstat"),
            branchScope,
            includePaths,
            excludePaths
        )
    }

    fun detailedCommand(
        startDate: String?,
        endDate: String?,
        excludePaths: List<String>,
        branchScope: BranchScope = BranchScope.CurrentBranch,
        includePaths: List<String> = emptyList()
    ): List<String> {
        return buildLogCommand(
            listOf(
                "--numstat",
                "--pretty=format:%x1f%h%x1f%ad%x1f%aN"
            ) + buildDateArgs(startDate, endDate),
            branchScope,
            includePaths,
            excludePaths
        )
    }

    private fun buildDateArgs(startDate: String?, endDate: String?): List<String> {
        return listOfNotNull(
            startDate?.let { "--since=$it" },
            endDate?.let { "--until=$it" }
        )
    }

    private fun buildLogCommand(
        options: List<String>,
        branchScope: BranchScope,
        includePaths: List<String>,
        excludePaths: List<String>
    ): List<String> {
        return listOf(gitExecutablePath, "log") +
            options +
            buildRevisionArgs(branchScope) +
            listOf("--") +
            buildPathspecArgs(includePaths, excludePaths)
    }

    private fun buildRevisionArgs(branchScope: BranchScope): List<String> {
        return when (branchScope) {
            BranchScope.CurrentBranch -> emptyList()
            BranchScope.Head -> listOf("HEAD")
            BranchScope.AllLocalBranches -> listOf("--branches")
            is BranchScope.SelectedBranch -> listOf(branchScope.refName)
            is BranchScope.SelectedBranches -> branchScope.branches.map { it.refName }
            is BranchScope.CustomRevisionRange -> listOf(branchScope.revisionRange)
        }
    }

    internal fun buildPathspecArgs(excludePaths: List<String>): List<String> {
        return buildPathspecArgs(emptyList(), excludePaths)
    }

    internal fun buildPathspecArgs(includePaths: List<String>, excludePaths: List<String>): List<String> {
        val includedPathspecs = includePaths.ifEmpty { listOf(".") }
        return includedPathspecs + excludePaths.map { ":(exclude)$it" }
    }
}
