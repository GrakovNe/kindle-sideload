package org.grakovne.sideload.kindle.shelf.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "shelf.web")
class ShelfWebProperties {
    var hostName: String by Delegates.notNull()
}