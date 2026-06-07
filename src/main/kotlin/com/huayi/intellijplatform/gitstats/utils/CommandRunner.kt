package com.huayi.intellijplatform.gitstats.utils

import java.util.concurrent.TimeUnit

interface CommandRunner {
    fun run(
        repoPath: String,
        command: List<String>,
        timeoutAmount: Long,
        timeUnit: TimeUnit
    ): CommandResult
}

object ProcessCommandRunner : CommandRunner {
    override fun run(
        repoPath: String,
        command: List<String>,
        timeoutAmount: Long,
        timeUnit: TimeUnit
    ): CommandResult {
        return Utils.runCommand(repoPath, command, timeoutAmount, timeUnit)
    }
}
