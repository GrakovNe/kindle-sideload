package org.grakovne.sideload.kindle.converer

import arrow.core.Either
import org.grakovne.sideload.kindle.binary.UserEnvironmentService
import org.grakovne.sideload.kindle.converer.binary.configuration.ConverterBinarySourceProperties
import org.grakovne.sideload.kindle.converer.binary.provider.ConverterBinaryProvider
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File

@Service
class ConverterService(
    private val userEnvironmentService: UserEnvironmentService,
    private val binaryProvider: ConverterBinaryProvider,
    private val properties: ConverterBinarySourceProperties
) {

    fun convertBook(
        userId: String,
        book: File
    ): Either<ConvertationError, ConversionResult> {

        val environment = userEnvironmentService
            .deployEnvironment(userId)
            .map { it.toPath().resolve(properties.converterFileName).toFile() }
            .tap {
                FileCopyUtils.copy(
                    binaryProvider.provideBinaryConverter(),
                    it
                )
            }

        return Either.Right(ConversionResult(null, null))
    }
}

data class ConversionResult(
    val output: File?,
    val conversionLog: String?
)