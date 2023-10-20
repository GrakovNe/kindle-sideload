package org.grakovne.sideload.kindle.converter.binary.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "converter.binary")
class ConverterBinaryProperties {
    var binaryPersistencePath: String by Delegates.notNull()

    var shell: String by Delegates.notNull()
    var shellArgs: String by Delegates.notNull()
    var converterFileName: String by Delegates.notNull()
    var configurationExtensions: List<String> by Delegates.notNull()
    var converterParameters: String by Delegates.notNull()
}