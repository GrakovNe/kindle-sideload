package org.grakovne.sideload.kindle.common

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExtensionsTest {

    @Test
    fun `parallelMap keeps the element order and applies the transform`() = runTest {
        val result = listOf(1, 2, 3, 4, 5).parallelMap { it * 10 }
        assertEquals(listOf(10, 20, 30, 40, 50), result)
    }

    @Test
    fun `parallelMap works on an empty list`() = runTest {
        assertEquals(emptyList<Int>(), emptyList<Int>().parallelMap { it })
    }

    @Test
    fun `ifTrue runs the action only when the value is true`() {
        var executed = 0

        true.ifTrue { executed++ }
        false.ifTrue { executed++ }

        assertEquals(1, executed)
    }
}
