package org.grakovne.sideload.kindle

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Base class for tests that exercise the services/DAOs against the real H2 test database.
 * The database and the [DSLContext] come from the shared application test context
 * (application.yml: in-memory H2 + Flyway migrations), so all tests in the JVM share the
 * same database. The [resetSharedTestDatabase] hook wipes every table before each test,
 * so subclasses can freely seed their fixtures.
 */
@SpringBootTest
open class TestDatabase {

    @Autowired
    lateinit var dsl: DSLContext

    @BeforeEach
    fun resetSharedTestDatabase() {
        checkTestDatabaseIsNotShared()
        val tables = dsl.select(DSL.field("table_name", String::class.java))
            .from(DSL.table("information_schema.tables"))
            .where(DSL.field("table_schema", String::class.java).eq("public"))
            .and(DSL.field("table_name", String::class.java).ne("flyway_schema_history"))
            .fetch()
            .map { it.value1() }
        tables.forEach { dsl.execute("truncate table \"$it\" restart identity") }
    }

    /**
     * The in-memory H2 database is shared by every test class in the JVM (and, when the
     * JDBC URL is switched to a file, by every Gradle worker fork as well), so the
     * per-test truncate below is only safe while the tests run sequentially. Fail fast
     * if that invariant is broken instead of corrupting the test run.
     */
    private fun checkTestDatabaseIsNotShared() {
        val parallelForks = System.getProperty("GRADLE_PARALLEL_WORKERS")
        require(parallelForks == null || parallelForks.toLongOrNull() == 1L) {
            "TestDatabase shares a single test database across all tests, but the build " +
                "runs the tests in parallel forks. Set maxParallelForks to 1 (it is the " +
                "default) or isolate the test database per fork."
        }
    }
}
