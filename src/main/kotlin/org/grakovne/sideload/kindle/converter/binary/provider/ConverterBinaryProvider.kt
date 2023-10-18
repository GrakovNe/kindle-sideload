package org.grakovne.sideload.kindle.converter.binary.provider

import mu.KotlinLogging
import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinarySourceProperties
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

@Service
class ConverterBinaryProvider(
    private val sourceProperties: ConverterBinarySourceProperties
) {

    fun provideBinaryConverter(): File = provideBinaryFolder()
        .toPath()
        .resolve(sourceProperties.converterFileName)
        .toFile()
        .also { logger.debug { "Provided the path to binary converter. Path is: ${it.absoluteFile}" } }

    fun provideBinaryFolder(): File = Path
        .of(sourceProperties.binaryPersistencePath)
        .toFile()
        .also { it.mkdirs() }
        .also { logger.debug { "Provided the path to binary directory. Path is: ${it.absoluteFile}" } }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}