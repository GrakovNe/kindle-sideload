package org.grakovne.sideload.kindle.common.platform

import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Only the branch that matches the host running the tests is exercised here —
 * this test suite is designed to run on 64-bit macOS / Linux dev machines and CI.
 * The other branches (Windows, 32-bit) are guarded by the same `SystemUtils` /
 * system-property reads and are not host-reachable without an OS switch.
 */
class PlatformServiceTest {

    private val sut = PlatformService()

    @Test
    fun `returns the platform binary name for the current 64-bit host`() {
        val expected = when {
            SystemUtils.IS_OS_MAC -> "darwin-arm64"

            SystemUtils.IS_OS_LINUX -> when {
                System.getProperty("os.arch").contains("arm") ||
                    System.getProperty("os.arch").contains("aarch64") -> "linux-arm64"

                System.getProperty("os.arch").contains("amd64") ||
                    System.getProperty("os.arch").contains("x86_64") -> "linux-amd64"

                else -> unsupportedHost()
            }

            else -> unsupportedHost()
        }

        val result = sut.fetchPlatformName()

        assertTrue(result.isRight())
        assertEquals(expected, result.orNull())
    }

    private fun unsupportedHost(): Nothing =
        throw AssertionError("This test can only run on 64-bit macOS or Linux hosts")
}
