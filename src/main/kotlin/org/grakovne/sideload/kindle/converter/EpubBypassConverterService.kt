package org.grakovne.sideload.kindle.converter

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File

@Service
class EpubBypassConverterService(
    private val userEnvironmentService: UserEnvironmentService,
    private val environmentProperties: EnvironmentProperties
) {

    fun convertAndCollect(
        userId: String,
        book: File
    ): Either<ConvertationError, ConversionResult> {
        logger.info { "Processing bypass convertation of ${book.name} for user id: $userId" }

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

        val environmentFiles = environment.snapshotDirectory()
        val inputFile = deployContent(environment, book)
        val outputFiles = dropVerboseFile(environment.snapshotDirectory() - environmentFiles.toSet())

        return Either.Right(
            ConversionResult(
                log = "Bypass conversion completed: ${inputFile.name}",
                environmentId = environment.name,
                output = outputFiles
            )
        ).also {
            logger.info {
                "The bypass convertation of ${book.name} for user id: $userId finished successfully. Output files are: ${outputFiles.map { it.name }}"
            }
        }
    }

    private fun dropVerboseFile(outputFiles: List<File>) =
        outputFiles.filter { environmentProperties.outputFileExtensions.contains(it.extension) }

    private fun File.snapshotDirectory() = this.listFiles()?.toList() ?: emptyList()

    private fun deployContent(
        environment: File,
        input: File
    ): File = environment
        .also { logger.debug { "Deploying the environment content from ${input.path} to the environment ${environment.path}" } }
        .let {
            val inputFile = it.toPath().resolve(input.name).toFile()
            FileCopyUtils.copy(input, inputFile)
            inputFile
        }

    private fun bypassBook(
        inputFile: File
    ): File {
        return inputFile
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}