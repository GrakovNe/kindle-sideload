package org.grakovne.sideload.kindle.binary

import arrow.core.Either
import org.grakovne.sideload.kindle.user.domain.User
import org.grakovne.sideload.kindle.binary.configuration.EnvironmentProperties
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

@Service
class UserEnvironmentService(
    private val environmentProperties: EnvironmentProperties
) {

    private fun provideBinaryFolder(): File = Path
        .of(environmentProperties.temporaryFolder)
        .toFile()
        .also { it.mkdirs() }

    fun deployEnvironment(user: User): Either<EnvironmentError, File> {
        val temporaryFolder = provideBinaryFolder()

        TODO()
    }

    fun terminateEnvironment(user: User): Either<EnvironmentError, File> = TODO()
}