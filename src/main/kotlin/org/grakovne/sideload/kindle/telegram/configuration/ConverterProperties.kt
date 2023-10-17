package org.grakovne.sideload.kindle.telegram.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "converter")
class ConverterProperties {

    var sourceFileExtensions: List<String> by Delegates.notNull()
}