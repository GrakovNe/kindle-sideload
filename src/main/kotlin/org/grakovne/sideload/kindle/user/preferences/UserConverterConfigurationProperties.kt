package org.grakovne.sideload.kindle.user.preferences

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "user.converter.configuration")
class UserConverterConfigurationProperties {

    var configurationsPath: String by Delegates.notNull()
    val configurationFileName: String by Delegates.notNull()
}