package org.grakovne.sideload.kindle.user.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "user.converter.configuration")
class UserConverterConfigurationProperties {

    var path: String by Delegates.notNull()
    var fileName: String by Delegates.notNull()
}