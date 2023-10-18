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
            .also { it.waitFor() }

        return if (process.exitValue() == 0) {
            Either.Right(BufferedReader(InputStreamReader(process.inputStream)).readLines().joinToString("\n"))
        } else {
            Either.Left(BufferedReader(InputStreamReader(process.inputStream)).readLines().joinToString("\n"))
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}