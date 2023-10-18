package org.grakovne.sideload.kindle.converter

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.CliRunner
import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinaryProperties
import org.grakovne.sideload.kindle.converter.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File


@Service
class ConverterService(
    private val cliRunner: CliRunner,
    private val userEnvironmentService: UserEnvironmentService,
    private val binaryProvider: ConverterBinaryProvider,
    private val properties: ConverterBinaryProperties
) {

    fun convertAndCollect(
        userId: String,
        book: File
    ): Either<ConvertationError, ConversionResult> {
        logger.info { "Processing the convertation of ${book.name} for user id: $userId" }

        val environment = userEnvironmentService
            .also { logger.debug { "Deploying temporary environment for $userId" } }
            .deployEnvironment(userId)
            .fold(
                ifLeft = { error ->
                    return Either
                        .Left(UnableDeployEnvironment)
                        .also { logger.error { "Unable to deploy environment for $userId. See details: $error" } }
                },
                ifRight = { it }
            )
            .also { deployContent(it, book) }

        val environmentFiles = environment.snapshotDirectory()
        val result = convertBook(environment)
        val outputFiles = environment.snapshotDirectory() - environmentFiles.toSet()

        return result
            .tap { logger.info { "The convertation of ${book.name} for user id: $userId finished successfully. Output files are: ${outputFiles.map { it.name }}" } }
            .map { ConversionResult(it, environment.name, outputFiles) }
            .mapLeft {
                UnableConvertFile(it, environment.name)
                    .also { logger.error { "The convertation of ${book.name} for user id: $userId failed. See details: $it" } }
            }
    }

    private fun File.fetchConfigurationFileName(): String? = this
        .listFiles()
        ?.find { it.name.endsWith(properties.configurationExtension) }
        ?.name

    private fun buildShellCommand(environment: File): String {
        val path = binaryProvider.provideBinaryConverter().absolutePath
        val configurationKey = environment.fetchConfigurationFileName()?.let { "-c $it" } ?: ""

        return "$path $configurationKey convert ${properties.converterParameters} $sourceFileInputName"
            .also { logger.debug { "Shell command build: $it" } }
    }

    private fun File.snapshotDirectory() = this.listFiles()?.toList() ?: emptyList()

    private fun deployContent(
        environment: File,
        input: File
    ) = environment
        .also { logger.debug { "Deploying the environment content from ${it.path} to the environment ${environment.path}" } }
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
            val runCommand = buildShellCommand(environment)
            logger.debug { "Running $runCommand on ${properties.shell} with args ${properties.shellArgs}" }

            cliRunner.runCli(
                properties.shell,
                properties.shellArgs,
                runCommand,
                it
            )
        }

    companion object {
        private const val sourceFileInputName = "input.fb2"
        private val logger = KotlinLogging.logger { }
    }
}

data class ConversionResult(
    val log: String,
    val environmentId: String,
    val output: List<File>
)