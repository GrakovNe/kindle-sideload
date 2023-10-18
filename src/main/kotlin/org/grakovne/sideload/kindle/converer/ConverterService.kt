package org.grakovne.sideload.kindle.converer

import arrow.core.Either
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

        val environment = userEnvironmentService
            .deployEnvironment(userId)
            .fold(
                ifLeft = { return Either.Left(ConvertationError.UNABLE_TO_DEPLOY_ENVIRONMENT) },
                ifRight = { it }
            )
            .also { deployContent(it, book) }


        val environmentFiles = snapshotDirectory(environment)
        val result = convertBook(environment, book)
        val outputFiles = snapshotDirectory(environment) - environmentFiles.toSet()

        return Either.Right(ConversionResult(result, outputFiles))
    }

    private fun snapshotDirectory(file: File) = file.listFiles()?.toList() ?: emptyList()


    private fun deployContent(
        environment: File,
        input: File
    ) = environment

        .also {
            it
                .toPath()
                .resolve(properties.converterFileName)
                .toFile()
                .setExecutable(true)
        }
        .also {
            FileCopyUtils.copy(
                input,
                it.toPath().resolve(sourceFileInputName).toFile()
            )
        }

    private fun convertBook(
        environment: File,
        book: File
    ) = environment
        .let {
            val path = binaryProvider.provideBinaryConverter().absolutePath
            cliRunner.runCli(
                properties.shell,
                properties.shellArgs,
                "$path -c configuration.toml convert $sourceFileInputName",
                it
            )
        }

    companion object {
        private const val sourceFileInputName = "input.fb2"
    }
}

data class ConversionResult(
    val log: String,
    val output: List<File>
)