package org.grakovne.sideload.kindle.converter

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.CliRunner
import org.grakovne.sideload.kindle.converter.binary.configuration.ConverterBinaryProperties
import org.grakovne.sideload.kindle.converter.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.grakovne.sideload.kindle.user.preferences.service.UserPreferencesService
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File


@Service
class ConverterService(
    private val cliRunner: CliRunner,
    private val userEnvironmentService: UserEnvironmentService,
    private val binaryProvider: ConverterBinaryProvider,
    private val binaryProperties: ConverterBinaryProperties,
    private val environmentProperties: EnvironmentProperties,
    private val userPreferencesService: UserPreferencesService
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

        val inputFile = deployContent(environment, book)

        val environmentFiles = environment.snapshotDirectory()
        val userPreferences = userPreferencesService.fetchPreferences(userId)
        val result = convertBook(inputFile, environment, userPreferences)
        val outputFiles = dropVerboseFile(userPreferences, environment.snapshotDirectory() - environmentFiles.toSet())

        return result
            .tap { logger.info { "The convertation of ${book.name} for user id: $userId finished successfully. Output files are: ${outputFiles.map { it.name }}" } }
            .map { ConversionResult(it, environment.name, outputFiles) }
            .mapLeft {
                UnableConvertFile(
                    details = it,
                    environmentId = environment.name
                )
                    .also { logger.error { "The convertation of ${book.name} for user id: $userId failed. See details: $it" } }
            }
    }

    private fun dropVerboseFile(
        userPreferences: UserPreferences,
        outputFiles: List<File>
    ) = if (userPreferences.debugMode.not()) {
        outputFiles.filter { environmentProperties.outputFileExtensions.contains(it.extension) }
    } else {
        outputFiles
    }

    private fun File.fetchConfigurationFileName(): String? = this
        .listFiles()
        ?.find { file -> binaryProperties.configurationExtensions.any { file.extension == it } }
        ?.name

    private fun buildShellCommand(
        inputFile: File,
        environment: File,
        userPreferences: UserPreferences
    ): String {
        val path = binaryProvider.provideBinaryConverter().absolutePath
        val configurationKey = environment.fetchConfigurationFileName()?.let { "-c $it" } ?: ""

        return "$path $configurationKey convert --to ${userPreferences.outputFormat.name.lowercase()} ${binaryProperties.converterParameters} ${inputFile.name}"
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
                .resolve(binaryProperties.converterFileName)
                .toFile()
                .setExecutable(true)
        }
        .let {
            val inputFile = it.toPath().resolve(input.name).toFile()

            FileCopyUtils.copy(
                input,
                it.toPath().resolve(input.name).toFile()
            )

            inputFile
        }

    private fun convertBook(
        inputFile: File,
        environment: File,
        configuration: UserPreferences
    ) = environment
        .let {
            val runCommand = buildShellCommand(inputFile, environment, configuration)
            logger.debug { "Running $runCommand on ${binaryProperties.shell} with args ${binaryProperties.shellArgs}" }

            cliRunner.runCli(
                binaryProperties.shell,
                binaryProperties.shellArgs,
                runCommand,
                it
            )
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

data class ConversionResult(
    val log: String,
    val environmentId: String,
    val output: List<File>
)