package org.grakovne.sideload.kindle.converer

import arrow.core.Either
import org.grakovne.sideload.kindle.binary.EnvironmentError
import org.grakovne.sideload.kindle.binary.UserEnvironmentService
import org.grakovne.sideload.kindle.common.CliRunner
import org.grakovne.sideload.kindle.converer.binary.configuration.ConverterBinarySourceProperties
import org.grakovne.sideload.kindle.converer.binary.provider.ConverterBinaryProvider
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File


@Service
class ConverterService(
    private val cliRunner: CliRunner,
    private val userEnvironmentService: UserEnvironmentService,
    private val binaryProvider: ConverterBinaryProvider,
    private val properties: ConverterBinarySourceProperties
) {

    fun processAndCollect(
        userId: String,
        book: File
    ): Either<ConvertationError, ConversionResult> {

        val environment = userEnvironmentService.deployEnvironment(userId)

        val result = convertBook(environment, book)

        return Either.Right(ConversionResult(null, null))
    }

    private fun convertBook(
        environment: Either<EnvironmentError, File>,
        book: File
    ) = environment
        .tap {
            FileCopyUtils.copy(
                binaryProvider.provideBinaryConverter(),
                it.toPath().resolve(properties.converterFileName).toFile()
            )
        }
        .tap {
            it
                .toPath()
                .resolve(properties.converterFileName)
                .toFile()
                .setExecutable(true)
        }
        .tap {
            FileCopyUtils.copy(
                book,
                it.toPath().resolve(sourceFileInputName).toFile()
            )
        }
        .map {
            val path = it.toPath().resolve(properties.converterFileName).toFile().absoluteFile

            cliRunner.runCli(
                properties.shell,
                properties.shellArgs,
                "$path -c configuration.toml convert --stk $sourceFileInputName",
                it
            )
        }

    companion object {
        private const val sourceFileInputName = "input.fb2"
    }
}

data class ConversionResult(
    val output: File?,
    val conversionLog: String?
)