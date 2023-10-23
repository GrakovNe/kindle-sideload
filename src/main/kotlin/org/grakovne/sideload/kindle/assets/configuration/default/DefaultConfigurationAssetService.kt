package org.grakovne.sideload.kindle.assets.configuration.default

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Files

@Service
class DefaultConfigurationAssetService {

    private var temporaryConfigurationFile: File? = null

    fun fetchDefaultConfiguration(): File? {
        var file = temporaryConfigurationFile

        if (null == file || !file.exists()) {
            file = Files.createTempFile("default_", "configuration.zip").toFile()
            file.writeBytes(getDefaultConfigurationResource().readAllBytes())

            temporaryConfigurationFile = file
        }

        return temporaryConfigurationFile
    }

    private fun getDefaultConfigurationResource(): InputStream {
        return ClassPathResource("assets/default_configuration.zip").inputStream
    }
}