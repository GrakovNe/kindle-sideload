package org.grakovne.sideload.kindle.converer.binary.provider

import org.grakovne.sideload.kindle.converer.binary.configuration.ConverterBinarySourceProperties
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

@Service
class ConverterBinaryProvider(
    private val sourceProperties: ConverterBinarySourceProperties
) {
    fun provideBinaryFolder(): File = Path
        .of(sourceProperties.binaryPersistencePath)
        .toFile()
        .also { it.mkdirs() }

}