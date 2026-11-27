package org.grakovne.sideload.kindle.stk.email.delivery

import com.pengrad.telegrambot.TelegramBot
import org.grakovne.sideload.kindle.KindleSideloadApplication
import org.grakovne.sideload.kindle.NoOpScheduling
import org.grakovne.sideload.kindle.stk.email.delivery.imap.MailboxReader
import org.grakovne.sideload.kindle.stk.email.delivery.imap.RawEmailMessage
import org.grakovne.sideload.kindle.stk.email.delivery.imap.ScanResult
import org.grakovne.sideload.kindle.stk.email.delivery.service.DeliveryFailureMonitorService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.atomic.AtomicLong

/**
 * Boots the real application on a random port and proves that a bounce picked
 * up by the monitor surfaces as a Prometheus series on /actuator/prometheus.
 */
@SpringBootTest(
    classes = [KindleSideloadApplication::class, NoOpScheduling::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["delivery.monitor.enabled=true"],
)
class DeliveryFailureMetricsEndpointTest {

    @MockitoBean
    private lateinit var telegramBot: TelegramBot

    @MockitoBean
    private lateinit var mailboxReader: MailboxReader

    @Autowired
    private lateinit var monitorService: DeliveryFailureMonitorService

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @Test
    fun `counter series appears on prometheus endpoint after a bounce is polled`() {
        assertTrue(scrape().doesNotContainSeriesFor("5.7.234"))

        givenMailboxContains(dsnBounce(uniqueUid(), "5.7.234", "brand new provider error"))

        monitorService.poll()

        val body = scrape()
        assertTrue(
            body.contains("# TYPE $PROMETHEUS_METRIC counter"),
            "missing TYPE header in scrape output",
        )
        assertTrue(
            seriesRegex("5.7.234", "1.0").containsMatchIn(body),
            "expected $PROMETHEUS_METRIC{reason=\"5.7.234\"} 1.0 in scrape output",
        )
    }

    @Test
    fun `exposes one series per reason cluster`() {
        givenMailboxContains(
            dsnBounce(uniqueUid(), "5.7.1", "attachments may be corrupted or may contain malicious content"),
            RawEmailMessage(
                folder = "INBOX",
                uid = uniqueUid(),
                subject = "There was a problem with the document(s) you sent to Kindle",
                body = "Your document(s) could not be delivered due to E001 - Unsupported File Format:",
            ),
        )

        monitorService.poll()

        val body = scrape()
        assertTrue(body.contains("""reason="malicious-content""""), "missing malicious-content series")
        assertTrue(body.contains("""reason="unsupported-format""""), "missing unsupported-format series")
    }

    @Test
    fun `counts the same bounce only once across polls`() {
        val uid = uniqueUid()
        givenMailboxContains(dsnBounce(uid, "5.7.235", "duplicate probe"))

        monitorService.poll()
        monitorService.poll()

        val body = scrape()
        assertTrue(
            seriesRegex("5.7.235", "1.0").containsMatchIn(body),
            "duplicate poll must not double-count",
        )
    }

    private fun scrape(): String {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port/actuator/prometheus")).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        check(response.statusCode() == 200) { "prometheus endpoint responded ${response.statusCode()}" }

        return response.body()
    }

    private fun String.doesNotContainSeriesFor(reason: String): Boolean =
        !contains("""reason="$reason"""")

    private fun seriesRegex(reason: String, value: String): Regex =
        Regex("""$PROMETHEUS_METRIC\{reason="${Regex.escape(reason)}",?\}\s+$value""")

    private fun givenMailboxContains(vararg messages: RawEmailMessage) {
        whenever(mailboxReader.scan(anyOrNull(), any())).thenAnswer { invocation ->
            val handler = invocation.getArgument<(RawEmailMessage) -> Unit>(1)
            messages.forEach(handler)
            ScanResult("INBOX", 1L, messages.maxOf { it.uid })
        }
    }

    private fun dsnBounce(uid: Long, status: String, diagnostic: String) = RawEmailMessage(
        folder = "INBOX",
        uid = uid,
        subject = "Undelivered Mail Returned to Sender",
        body = """
            Final-Recipient: rfc822; reader@kindle.com
            Action: failed
            Status: $status
            Diagnostic-Code: smtp; 550 $status $diagnostic
        """.trimIndent(),
    )

    private fun uniqueUid(): Long = UID_SEQUENCE.incrementAndGet()

    companion object {
        private const val PROMETHEUS_METRIC = "kindle_sideload_delivery_failures_total"
        private val UID_SEQUENCE = AtomicLong(900_000L)
    }
}
