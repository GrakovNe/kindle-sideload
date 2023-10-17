package org.grakovne.sideload.kindle.converer

import arrow.core.Either
import org.grakovne.sideload.kindle.binary.UserEnvironmentService
import org.grakovne.sideload.kindle.converer.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.springframework.stereotype.Service
import java.io.File

@Service
class ConverterService(
    private val userEnvironmentService: UserEnvironmentService,
    private val binaryProvider: ConverterBinaryProvider
) {

    fun convertBook(
        user: User,
        book: File
    ): Either<ConvertationError, ConversionResult> {

        val f = userEnvironmentService.deployEnvironment(user)

        println(f)

        return Either.Right(ConversionResult(null, null))
    }
}

data class ConversionResult(
    val output: File?,
    val conversionLog: String?
)