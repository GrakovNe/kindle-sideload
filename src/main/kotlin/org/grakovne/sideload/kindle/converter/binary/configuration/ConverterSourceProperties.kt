package org.grakovne.sideload.kindle.converter.binary.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "converter.source")
class ConverterSourceProperties {
    var releasesUrl: String by Delegates.notNull()
}