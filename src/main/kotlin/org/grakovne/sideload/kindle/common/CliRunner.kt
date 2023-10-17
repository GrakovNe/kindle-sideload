package org.grakovne.sideload.kindle.common

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
    ): String {
        val process = ProcessBuilder(shell, shellArgs, command)
            .directory(directory)
            .redirectErrorStream(true)
            .start()
            .also { it.waitFor() }

        val exitValue = process.exitValue()

        return if (exitValue == 0) {
            BufferedReader(InputStreamReader(process.inputStream)).readLines().joinToString("\n")
        } else {
            BufferedReader(InputStreamReader(process.errorStream)).readLines().joinToString("\n")
        }
    }
}