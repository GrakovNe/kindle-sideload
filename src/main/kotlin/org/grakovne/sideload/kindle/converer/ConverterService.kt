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
                ifLeft = { return Either.Left(UnableDeployEnvironment) },
                ifRight = { it }
            )
            .also { deployContent(it, book) }


        val environmentFiles = environment.snapshotDirectory()
        val result = convertBook(environment)
        val outputFiles = environment.snapshotDirectory() - environmentFiles.toSet()

        return result
            .map { ConversionResult(it, outputFiles) }
            .mapLeft { UnableConvertFile(it) }
    }

    private fun File.fetchConfigurationFileName(): String? = this
        .listFiles()
        ?.find { it.name.endsWith(properties.configurationExtension) }
        ?.name

    private fun buildShellCommand(environment: File): String {
        val path = binaryProvider.provideBinaryConverter().absolutePath
        val configurationKey = environment.fetchConfigurationFileName()?.let { "-c $it" } ?: ""

        val result = "$path $configurationKey convert $sourceFileInputName"
        return result
    }

    private fun File.snapshotDirectory() = this.listFiles()?.toList() ?: emptyList()

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

    private fun convertBook(environment: File) = environment
        .let {
            cliRunner.runCli(
                properties.shell,
                properties.shellArgs,
                buildShellCommand(environment),
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