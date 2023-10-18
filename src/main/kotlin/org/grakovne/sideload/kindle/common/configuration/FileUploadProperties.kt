package org.grakovne.sideload.kindle.common.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "file.upload")
class FileUploadProperties {
    var maxSize: Long by Delegates.notNull()
}