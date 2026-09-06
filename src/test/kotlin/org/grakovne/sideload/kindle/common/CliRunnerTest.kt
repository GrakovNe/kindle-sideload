package org.grakovne.sideload.kindle.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CliRunnerTest {

    @TempDir
    lateinit var workingDir: File

    private val shell = "/bin/bash"
    private val sut = CliRunner()

    @Test
    fun `captures the combined output of a successful command`() {
        val result = sut.runCli(shell, "-c", "echo line one; echo line two", workingDir)

        assertTrue(result.isRight())
        assertEquals("line one\nline two", result.getOrNull())
    }

    @Test
    fun `works relative to the given working directory`() {
        val marker = File(workingDir, "marker.txt").apply { writeText("i am here") }

        val result = sut.runCli(shell, "-c", "cat marker.txt", workingDir)

        assertTrue(result.isRight())
        assertEquals("i am here", result.getOrNull()?.trim())
    }

    @Test
    fun `returns the output on the left side when the command fails`() {
        val result = sut.runCli(shell, "-c", "echo boom; exit 3", workingDir)

        assertTrue(result.isLeft())
        assertEquals("boom", result.swap().getOrNull())
    }

    @Test
    fun `returns left when the command is not found`() {
        val result = sut.runCli(shell, "-c", "definitely-not-a-real-command-xyz", workingDir)

        assertTrue(result.isLeft())
        val output = result.swap().getOrNull()
        assertNotNull(output)
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `joins multi-line output with line breaks`() {
        val result = sut.runCli(shell, "-c", "seq 1 3", workingDir)

        assertTrue(result.isRight())
        val output = result.getOrNull()
        assertEquals("1\n2\n3", output)
        assertFalse(output.isNullOrBlank())
    }
}
