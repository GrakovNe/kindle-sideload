package org.grakovne.sideload.kindle.common.configuration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.web.client.ResourceAccessException
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import kotlin.test.assertFailsWith

class RestTemplateConfigurationTest {

    @Test
    @Timeout(10)
    fun `applies the read timeout so a stalled server cannot hang the caller`() {
        // Reproduces the production wedge: the TCP connection is accepted but the server never
        // answers. Without a read timeout the request blocks forever on the scheduler thread;
        // with one it fails fast with a ResourceAccessException.
        val server = ServerSocket(0)
        val accepted = java.util.Collections.synchronizedList(mutableListOf<Socket>())
        val acceptor = Thread {
            runCatching {
                while (!server.isClosed) {
                    accepted += server.accept() // accept the connection but never respond
                }
            }
        }
            .apply { isDaemon = true }
            .also { it.start() }

        try {
            val restTemplate = RestTemplateConfiguration(
                Duration.ofMillis(500),
                Duration.ofMillis(500)
            ).restTemplate(RestTemplateBuilder())

            assertFailsWith<ResourceAccessException> {
                restTemplate.getForObject("http://127.0.0.1:${server.localPort}/", String::class.java)
            }
        } finally {
            accepted.forEach { runCatching { it.close() } }
            runCatching { server.close() }
        }
    }
}
