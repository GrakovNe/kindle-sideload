package org.grakovne.sideload.kindle

import org.junit.jupiter.api.Test

/**
 * Smoke test that the full application context starts against the shared embedded
 * PostgreSQL test database (see [TestDatabase]).
 */
class KindleSideloadApplicationTests : TestDatabase() {

    @Test
    fun contextLoads() {
    }
}
