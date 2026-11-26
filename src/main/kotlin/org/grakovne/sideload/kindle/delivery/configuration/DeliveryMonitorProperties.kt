package org.grakovne.sideload.kindle.delivery.configuration

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
}
