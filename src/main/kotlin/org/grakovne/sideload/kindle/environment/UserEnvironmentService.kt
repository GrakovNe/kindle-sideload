package org.grakovne.sideload.kindle.environment

import arrow.core.Either
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.grakovne.sideload.kindle.common.ZipArchiveService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.grakovne.sideload.kindle.user.configuration.domain.ConfigurationNotFoundError
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

@Service
class UserEnvironmentService(
    private val userConverterConfigurationService: UserConverterConfigurationService,
    private val environmentProperties: EnvironmentProperties,
    private val zipArchiveService: ZipArchiveService
) {

    fun provideEnvironmentFiles(environmentId: String) = provideEnvironmentFolder(environmentId)
        .listFiles()
        ?.filter { environmentProperties.outputFileExtensions.contains(it.extension) }
        ?: emptyList()

    fun deployEnvironment(userId: String): Either<EnvironmentError, File> {
        logger.info { "Deploying temporary environment for user: $userId" }

        val temporaryFolder = provideEnvironmentFolder(RandomStringUtils.randomAlphabetic(8))

        return userConverterConfigurationService
            .fetchConverterConfiguration(userId)
            .fold(
                ifLeft = {
                    when (it) {
                        ConfigurationNotFoundError -> Either.Right(null)
                        else -> Either.Left(it)
                    }
                },
                ifRight = { Either.Right(it) }
            )
            .map { it?.let { file -> zipArchiveService.unpack(file, temporaryFolder) } }
            .map { temporaryFolder }
            .mapLeft {
                logger.error { "Unable to deploy environment for user $userId. See details: $it" }
                UnableDeployError
            }
    }

    fun terminateEnvironment(environmentId: String): Either<EnvironmentError, Unit> =
        provideEnvironmentFolder(environmentId)
            .also { logger.info { "Terminating temporary environment id: $environmentId" } }
            .deleteRecursively()
            .let {
                when (it) {
                    true -> Either.Right(Unit)
                        .also { logger.info { "Terminated temporary environment id: $environmentId" } }

                    false -> Either.Left(UnableTerminateError)
                        .also { logger.error { "Unable to terminate temporary environment id: $environmentId" } }
                }
            }

    fun provideTemporaryEnvironmentsFolder(): File = Path
        .of(environmentProperties.temporaryFolder)
        .toFile()
        .also { it.mkdirs() }

    private fun provideEnvironmentFolder(environmentId: String): File = provideTemporaryEnvironmentsFolder()
        .toPath()
        .resolve(environmentId)
        .toFile()
        .also { it.mkdirs() }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}