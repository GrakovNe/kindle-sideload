package org.grakovne.sideload.kindle

import org.jooq.DSLContext
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
        val tables = dsl.fetch(
            """select table_name from information_schema.tables
               where table_schema = 'public' and table_name <> 'flyway_schema_history'"""
        ).map { record -> record.get("table_name", String::class.java) }
        tables.forEach { dsl.execute("truncate table \"$it\" restart identity") }
    }
}
