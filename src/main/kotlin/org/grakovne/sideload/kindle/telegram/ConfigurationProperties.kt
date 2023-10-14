package org.grakovne.sideload.kindle.telegram

import org.grakovne.sideload.kindle.events.internal.LogLevel
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "telegram")
class ConfigurationProperties {
    var token: String by Delegates.notNull()
    var level: LogLevel by Delegates.notNull()
}