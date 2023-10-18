package org.grakovne.sideload.kindle.environment.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "environment")
class EnvironmentProperties {

    var temporaryFolder: String by Delegates.notNull()
}