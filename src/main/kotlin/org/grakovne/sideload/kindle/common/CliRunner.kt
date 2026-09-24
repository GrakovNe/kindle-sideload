package org.grakovne.sideload.kindle.common

import arrow.core.Either
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@Service
class CliRunner {

    fun runCli(
        shell: String,
        shellArgs: String,
        command: String,
        directory: File,
        timeout: Duration = DEFAULT_TIMEOUT
    ): Either<String, String> {
        val process = ProcessBuilder(shell, shellArgs, command)
            .directory(directory)
            .redirectErrorStream(true)
            .start()
            .also { logger.debug { "Started a executable process ${it.pid()}" } }

        // Drain the merged stdout/stderr on a separate thread while waiting. The stream is backed by
        // a bounded OS pipe: a chatty converter that writes past the pipe buffer blocks on write, so
        // reading it only after waitFor() would deadlock the child. Draining concurrently and waiting
        // with a timeout keeps the shared scheduler thread from hanging forever on a wedged process.
        val output = AtomicReference("")
        val drain = Thread {
            output.set(
                runCatching {
                    process.inputStream.bufferedReader().use { it.readLines().joinToString("\n") }
                }.getOrDefault("")
            )
        }
            .apply {
                isDaemon = true
                name = "cli-runner-drain-${process.pid()}"
                start()
            }

        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            drain.join(DRAIN_JOIN_MILLIS)
            logger.error { "Executable process ${process.pid()} timed out after $timeout and has been killed" }

            return Either.Left("Command timed out after $timeout. Partial output:\n${output.get()}")
        }

        drain.join()
        val result = output.get()
        val exitCode = process.exitValue()

        return if (exitCode == 0) {
            logger.debug { "Executable process ${process.pid()} has been finished successfully. Exit code = 0" }
            Either.Right(result)
        } else {
            logger.error { "Executable process ${process.pid()} has been failed. Exit code = $exitCode" }
            logger.error { "Executable process ${process.pid()} error output is: $result" }

            Either.Left(result)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val DEFAULT_TIMEOUT: Duration = Duration.ofMinutes(10)
        private const val DRAIN_JOIN_MILLIS = 5_000L
    }
}
