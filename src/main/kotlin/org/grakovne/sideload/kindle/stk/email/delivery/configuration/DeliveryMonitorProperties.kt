package org.grakovne.sideload.kindle.stk.email.delivery.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "delivery.monitor")
class DeliveryMonitorProperties {
    var enabled: Boolean = false
    var host: String = "imap.mail.me.com"
    var port: Int = 993
    var username: String = ""
    var password: String = ""
    var folder: String = "INBOX"

    /**
     * Whether a mailbox without a stored baseline is scanned from its first message.
     * Off by default: the first poll then starts at the newest message instead of
     * re-reading years of history and counting old bounces as fresh ones.
     */
    var backfillHistory: Boolean = false
}
