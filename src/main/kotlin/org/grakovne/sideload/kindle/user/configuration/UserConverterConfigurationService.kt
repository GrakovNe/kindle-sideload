package org.grakovne.sideload.kindle.user.configuration

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.user.configuration.domain.UserConverterConfigurationError
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.IOException
import java.nio.file.Path

@Service
class UserConverterConfigurationService(
    private val properties: UserConverterConfigurationProperties
) {

    fun fetchConverterConfiguration(userId: String): Either<UserConverterConfigurationError, File> {
        logger.info { "Fetching converter configuration for $userId" }

        val asset = provideConfigurationAsset(userId)

        return when (asset.exists()) {
            true -> Either.Right(asset).also { logger.debug { "Found requested configuration file for user $userId" } }
            false -> Either
                .Left(UserConverterConfigurationError.CONFIGURATION_NOT_FOUND)
                .also { logger.info { "Requested configuration file for user $userId was not found" } }
        }
    }

    fun updateConverterConfiguration(user: User, configuration: File): Either<UserConverterConfigurationError, File> {
        val asset = provideConfigurationAsset(user.id)

        return try {
            FileCopyUtils
                .copy(configuration, asset)
                .let { Either.Right(asset) }
        } catch (ex: IOException) {
            return Either.Left(UserConverterConfigurationError.UNABLE_UPDATE_CONFIGURATION)
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