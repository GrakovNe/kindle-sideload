package org.grakovne.sideload.kindle.binary

import arrow.core.Either
import org.apache.commons.lang3.RandomStringUtils
import org.grakovne.sideload.kindle.binary.configuration.EnvironmentProperties
import org.grakovne.sideload.kindle.common.ZipArchiveService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
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
        val temporaryFolder = provideBinaryFolder()
            .toPath()
            .resolve(RandomStringUtils.randomAlphabetic(8))
            .toFile()
            .also { it.mkdirs() }

        return userConverterConfigurationService
            .fetchConverterConfiguration(userId)
            .map { zipArchiveService.unpack(it, temporaryFolder) }
            .map { temporaryFolder }
            .mapLeft { EnvironmentError.UNABLE_TO_DEPLOY }
    }

    fun terminateEnvironment(userId: String): Either<EnvironmentError, Unit> {
        val temporaryFolder = provideBinaryFolder()
            .toPath()
            .resolve(userId)
            .toFile()
            .also { it.mkdirs() }

        return temporaryFolder
            .delete()
            .let { Either.Right(Unit) }
    }
}