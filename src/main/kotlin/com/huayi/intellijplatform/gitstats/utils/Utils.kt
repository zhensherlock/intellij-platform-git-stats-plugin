package com.huayi.intellijplatform.gitstats.utils

import com.intellij.openapi.diagnostic.thisLogger
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class CommandResult(
    val output: String = "",
    val exitCode: Int? = null,
    val timedOut: Boolean = false,
    val exceptionMessage: String? = null,
    val command: List<String> = emptyList()
) {
    val isSuccess: Boolean
        get() = !timedOut && exceptionMessage == null && exitCode == 0

    fun details(): String = buildList {
        if (timedOut) add("Command timed out.")
        if (exitCode != null) add("Exit code: $exitCode")
        if (!exceptionMessage.isNullOrBlank()) add("Exception: $exceptionMessage")
        if (output.isNotBlank()) add(output.trim())
    }.joinToString("\n")
}

object Utils {
    fun checkDirectoryExists(directoryPath: String?): Boolean {
        if (directoryPath.isNullOrEmpty()) {
            return false
        }
        val directory = File(directoryPath)
        return directory.exists() && directory.isDirectory
    }

    fun runCommand(
        repoPath: String,
        cmd: List<String>,
        timeoutAmount: Long = 60L,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): CommandResult {
        return runCatching {
            val process = ProcessBuilder(cmd)
                .directory(File(repoPath))
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
            val outputBuffer = StringBuffer()
            val outputReader = thread(isDaemon = true, name = "GitStats command output reader") {
                process.inputStream.bufferedReader().use { reader ->
                    outputBuffer.append(reader.readText())
                }
            }
            val finished = process.waitFor(timeoutAmount, timeUnit)
            if (!finished) {
                process.destroy()
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                }
            }
            outputReader.join(2_000)
            CommandResult(
                output = outputBuffer.toString(),
                exitCode = if (finished) process.exitValue() else null,
                timedOut = !finished,
                command = cmd
            )
        }.onFailure {
            thisLogger().info("Failed to run command: ${cmd.joinToString(" ")}", it)
        }.getOrElse {
            CommandResult(
                exceptionMessage = it.message ?: it::class.java.simpleName,
                command = cmd
            )
        }
    }

    fun runCommand(repoPath: String, vararg cmd: String): CommandResult = runCommand(repoPath, listOf(*cmd))
}
