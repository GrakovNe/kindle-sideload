package org.grakovne.sideload.kindle.converer.binary.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "converter.binary")
class ConverterBinarySourceProperties {
    var releasesUrl: String by Delegates.notNull()
    var extension: String by Delegates.notNull()
    var binaryPersistencePath: String by Delegates.notNull()

    var shell: String by Delegates.notNull()
    var shellArgs: String by Delegates.notNull()
    var runCommand: String by Delegates.notNull()
    var converterFileName: String by Delegates.notNull()
}