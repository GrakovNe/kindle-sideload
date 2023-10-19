package org.grakovne.sideload.kindle.user.configuration

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.user.configuration.domain.ConfigurationNotFoundError
import org.grakovne.sideload.kindle.user.configuration.domain.UnableUpdateConfigurationError
import org.grakovne.sideload.kindle.user.configuration.domain.UserConverterConfigurationError
import org.grakovne.sideload.kindle.user.configuration.domain.ValidationError
import org.grakovne.sideload.kindle.user.configuration.validation.ConfigurationValidationService
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.IOException
import java.nio.file.Path

@Service
class UserConverterConfigurationService(
    private val properties: UserConverterConfigurationProperties,
    private val validationService: ConfigurationValidationService
) {

    fun fetchConverterConfiguration(userId: String): Either<UserConverterConfigurationError, File> {
        logger.info { "Fetching converter configuration for $userId" }

        val asset = provideConfigurationAsset(userId)

        return when (asset.exists()) {
            true -> Either.Right(asset).also { logger.debug { "Found requested configuration file for user $userId" } }
            false -> Either
                .Left(ConfigurationNotFoundError)
                .also { logger.info { "Requested configuration file for user $userId was not found" } }
        }
    }

    fun removeConverterConfiguration(userId: String): Either<UserConverterConfigurationError, Unit> {
        return provideConfigurationAsset(userId)
            .also { logger.debug { "Removing user configuration asset" } }
            .deleteRecursively()
            .let {
                when (it) {
                    true -> Either
                        .Right(Unit)
                        .also { logger.debug { "Removed user configuration asset" } }

                    false -> Either
                        .Left(UnableUpdateConfigurationError)
                        .also { logger.warn { "User configuration asset was not removed and still using" } }
                }
            }
    }

    fun updateConverterConfiguration(user: User, configuration: File): Either<UserConverterConfigurationError, File> {
        val asset = validationService.validate(configuration)
            .fold(
                ifLeft = {
                    return Either.Left(ValidationError(it.code))
                },
                ifRight = { provideConfigurationAsset(user.id) }
            )

        return try {
            FileCopyUtils
                .copy(configuration, asset)
                .let { Either.Right(asset) }
        } catch (ex: IOException) {
            return Either.Left(UnableUpdateConfigurationError)
        }
    }

    private fun provideConfigurationAsset(userId: String) = Path
        .of(properties.path)
        .resolve(userId)
        .toFile()
        .also { it.mkdirs() }
        .toPath()
        .resolve(properties.fileName)
        .toFile()

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}