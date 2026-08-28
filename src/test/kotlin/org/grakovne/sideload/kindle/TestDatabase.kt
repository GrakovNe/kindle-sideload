package org.grakovne.sideload.kindle

import com.pengrad.telegrambot.TelegramBot
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory
import org.springframework.test.context.MergedContextConfiguration
import org.springframework.test.context.TestContext

/**
 * The single embedded PostgreSQL instance shared by every test context in the JVM. Every
 * test context reads the url/credentials from this instance (via
 * [TestDatabaseContextCustomizer]), so all Spring test contexts in the same JVM hit the
 * same database.
 */
object TestPostgres {
    val embeddedPostgres: io.zonky.test.db.postgres.embedded.EmbeddedPostgres =
        io.zonky.test.db.postgres.embedded.EmbeddedPostgres.builder().start()
}

/**
 * Wires the datasource properties of the shared [TestPostgres] instance into every Spring
 * test context of this module (registered for the whole test source set, see
 * META-INF/spring.factories), so the tests' application.yml does not hardcode a datasource.
 */
class TestDatabaseContextCustomizer : ContextCustomizer {
    override fun customizeContext(context: org.springframework.context.ConfigurableApplicationContext, macc: MergedContextConfiguration) {
        val properties = mapOf(
            "spring.datasource.url" to TestPostgres.embeddedPostgres.getJdbcUrl("postgres", "postgres"),
            "spring.datasource.username" to "postgres",
            "spring.datasource.password" to "postgres",
            "spring.datasource.driver-class-name" to "org.postgresql.Driver"
        )
        context.environment.propertySources.addFirst(MapPropertySource("embeddedTestPostgres", properties))
    }
}

class TestDatabaseContextCustomizerFactory : ContextCustomizerFactory {
    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: List<ContextConfigurationAttributes>
    ): ContextCustomizer? = TestDatabaseContextCustomizer()
}

/**
 * Base class for tests that exercise the services/DAOs against a real embedded PostgreSQL
 * database. The database and the [DSLContext] come from the shared application test context
 * (application.yml: embedded PostgreSQL + Flyway migrations), so all tests in the JVM share
 * the same database. The [resetSharedTestDatabase] hook wipes every table before each test,
 * so subclasses can freely seed their fixtures.
 *
 * This is a full [SpringBootTest], not a slice, so everything "outside" the application has to
 * be neutralised explicitly or it would run against the shared database and the network:
 *
 *  - [NoOpScheduling] parks the `@Scheduled` workers. Otherwise the 100 ms
 *    `ConvertSourceFilePeriodicService` and the 5 s `StkEmailPeriodicService` would pick up the
 *    ACTIVE tasks a test has just seeded and move them to FAILED before its assertions run, and
 *    `ConverterBinaryPeriodicUpdateTask` would call the GitHub releases API for real;
 *  - [TelegramBot] is mocked, because `MessageListenersConfiguration.onCreate()` registers an
 *    updates listener in `@PostConstruct`, which starts long-polling api.telegram.org for the
 *    whole test run.
 *
 * Tests that need a periodic worker to run inject it and call it directly.
 */
@SpringBootTest(classes = [KindleSideloadApplication::class, NoOpScheduling::class])
open class TestDatabase {

    @Autowired
    lateinit var dsl: DSLContext

    @MockBean
    private lateinit var telegramBot: TelegramBot

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
     * The embedded PostgreSQL database is shared by every test class in the JVM (and, when
     * multiple Gradle worker forks are used, each fork gets its own embedded instance on a
     * distinct port), so the per-test truncate below is only safe while the tests run
     * sequentially. Fail fast if that invariant is broken instead of corrupting the test run.
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
