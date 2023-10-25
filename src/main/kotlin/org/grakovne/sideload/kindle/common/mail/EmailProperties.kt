package org.grakovne.sideload.kindle.common.mail

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "spring.mail")
class EmailProperties {
    var from: String by Delegates.notNull()
    var subject: String by Delegates.notNull()
    var text: String by Delegates.notNull()
}