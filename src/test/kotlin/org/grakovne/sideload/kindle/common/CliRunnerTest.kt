package org.grakovne.sideload.kindle.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration
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

    @Test
    @Timeout(30)
    fun `does not deadlock when the command writes more than the os pipe buffer`() {
        // A chatty converter (e.g. fb2cng on a messy book) writes far more than the ~64 KiB
        // pipe buffer. Reading the output only after waitFor() deadlocks the child on write,
        // so runCli must drain the stream while the process runs.
        val result = sut.runCli(
            shell,
            "-c",
            "for i in \$(seq 1 40000); do echo \"line \$i padded out to exceed the pipe buffer\"; echo \"warn \$i\" 1>&2; done",
            workingDir
        )

        assertTrue(result.isRight())
        val output = result.getOrNull()!!
        val lines = output.lines()
        assertEquals(80000, lines.size, "both stdout and stderr must be captured in full")
        assertTrue(lines.contains("line 40000 padded out to exceed the pipe buffer"))
        assertTrue(lines.contains("warn 40000"))
    }

    @Test
    @Timeout(20)
    fun `kills the process and returns left when it runs past the timeout`() {
        // A wedged converter (e.g. fbc stuck on a malformed book) must not block the shared
        // scheduler thread forever: runCli bounds the wait and force-kills the child on timeout.
        val started = System.nanoTime()

        val result = sut.runCli(shell, "-c", "sleep 30", workingDir, timeout = Duration.ofMillis(500))

        val elapsedMillis = (System.nanoTime() - started) / 1_000_000
        assertTrue(result.isLeft())
        assertTrue(result.swap().getOrNull()!!.contains("timed out"))
        assertTrue(elapsedMillis < 10_000, "runCli returned only after ${elapsedMillis}ms instead of killing the child")
    }

    @Test
    @Timeout(20)
    fun `returns the partial output of a process killed on timeout`() {
        val result = sut.runCli(shell, "-c", "echo partial-marker; sleep 30", workingDir, timeout = Duration.ofMillis(1_000))

        assertTrue(result.isLeft())
        val left = result.swap().getOrNull()!!
        assertTrue(left.contains("partial-marker"), "expected the output written before the hang to be captured, got: $left")
    }
}
