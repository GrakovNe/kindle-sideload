package org.grakovne.sideload.kindle.common

import arrow.core.Either
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Service
class CliRunner {

    fun runCli(
        shell: String,
        shellArgs: String,
        command: String,
        directory: File
    ): Either<String, String> {
        val process = ProcessBuilder(shell, shellArgs, command)
            .directory(directory)
            .redirectErrorStream(true)
            .start()
            .also { logger.debug { "Started a executable process ${it.pid()}" } }

        // Drain the merged stdout/stderr before waiting. The stream is backed by a bounded OS
        // pipe: a chatty converter that writes more than the pipe buffer blocks on write, so
        // waiting for the exit code first would deadlock the process (and the shared scheduler).
        val output = BufferedReader(InputStreamReader(process.inputStream))
            .use { it.readLines() }
            .joinToString("\n")

        val exitCode = process.waitFor()

        return if (exitCode == 0) {
            logger.debug { "Executable process ${process.pid()} has been finished successfully. Exit code = 0" }
            Either.Right(output)
        } else {
            logger.error { "Executable process ${process.pid()} has been failed. Exit code = $exitCode" }
            logger.error { "Executable process ${process.pid()} error output is: $output" }

            Either.Left(output)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
