package org.grakovne.sideload.kindle.common.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Bounds for every outbound HTTP call made through the shared `RestTemplate`
 * (the GitHub converter-binary release checks and the Telegram book downloads).
 *
 * The defaults exist so a stalled connection fails fast instead of parking a
 * scheduler thread forever; they can be overridden via `http.client.*`.
 */
@Configuration
@ConfigurationProperties(prefix = "http.client")
class OutboundHttpClientProperties {
    var connectTimeout: Duration = Duration.ofSeconds(5)
    var readTimeout: Duration = Duration.ofSeconds(60)
}
