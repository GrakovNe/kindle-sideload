package org.grakovne.sideload.kindle.environment

import arrow.core.Either
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.grakovne.sideload.kindle.common.ZipArchiveService
import org.grakovne.sideload.kindle.environment.configuration.EnvironmentProperties
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.grakovne.sideload.kindle.user.configuration.domain.UserConverterConfigurationError
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

@Service
class UserEnvironmentService(
    private val userConverterConfigurationService: UserConverterConfigurationService,
    private val environmentProperties: EnvironmentProperties,
    private val zipArchiveService: ZipArchiveService
) {

    private fun provideBinaryFolder(): File = Path
        .of(environmentProperties.temporaryFolder)
        .toFile()
        .also { it.mkdirs() }

    fun deployEnvironment(userId: String): Either<EnvironmentError, File> {
        val temporaryFolder = provideEnvironmentFolder(RandomStringUtils.randomAlphabetic(8))

        return userConverterConfigurationService
            .fetchConverterConfiguration(userId)
            .fold(
                ifLeft = {
                    when (it) {
                        UserConverterConfigurationError.CONFIGURATION_NOT_FOUND -> Either.Right(null)
                        else -> Either.Left(it)
                    }
                },

                ifRight = { Either.Right(it) }
            )
            .map { it?.let { file -> zipArchiveService.unpack(file, temporaryFolder) } }
            .map { temporaryFolder }
            .mapLeft { EnvironmentError.UNABLE_TO_DEPLOY }
    }

    fun terminateEnvironment(environmentId: String): Either<EnvironmentError, Unit> =
        provideEnvironmentFolder(environmentId)
            .deleteRecursively()
            .let { Either.Right(Unit) }

    private fun provideEnvironmentFolder(environmentId: String) = provideBinaryFolder()
        .toPath()
        .resolve(environmentId)
        .toFile()
        .also { it.mkdirs() }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}