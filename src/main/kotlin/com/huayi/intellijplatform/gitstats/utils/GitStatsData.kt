package com.huayi.intellijplatform.gitstats.utils

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
    var addedLines: Int = 0,
    var deletedLines: Int = 0,
    var fileName: String
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
