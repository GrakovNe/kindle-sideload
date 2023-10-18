package org.grakovne.sideload.kindle.converter.binary.provider

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

    fun provideBinaryFolder(): File = Path
        .of(sourceProperties.binaryPersistencePath)
        .toFile()
        .also { it.mkdirs() }

}