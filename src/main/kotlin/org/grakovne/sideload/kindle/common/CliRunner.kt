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
            .also { it.waitFor() }

        return if (process.exitValue() == 0) {
            logger.debug { "Executable process ${process.pid()} has been finished successfully. Exit code = 0" }
            Either.Right(BufferedReader(InputStreamReader(process.inputStream)).readLines().joinToString("\n"))
        } else {
            val errorResult = BufferedReader(InputStreamReader(process.inputStream)).readLines().joinToString("\n")
            logger.error { "Executable process ${process.pid()} has been failed. Exit code = ${process.exitValue()}" }
            logger.error { "Executable process ${process.pid()} error output is: $errorResult" }

            Either.Left(errorResult)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}