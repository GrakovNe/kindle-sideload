package org.grakovne.sideload.kindle.converer.binary.unpack

import arrow.core.Either
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.grakovne.sideload.kindle.converer.binary.configuration.ConverterBinarySourceProperties
import org.grakovne.sideload.kindle.converer.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.converer.binary.reference.domain.BinaryError
import org.springframework.stereotype.Service
import java.io.File


@Service
class ArchivedBinaryUnpackService(
    private val sourceProperties: ConverterBinarySourceProperties,
    private val binaryProvider: ConverterBinaryProvider
) {

    fun unpack(archive: File): Either<BinaryError, Unit> {
        if (sourceProperties.extension.endsWith("zip").not()) {
            return Either.Left(BinaryError.UNABLE_TO_UNPACK_BINARY)
        }

        try {
            ZipFile(archive).extractAll(binaryProvider.provideBinaryFolder().absolutePath)
        } catch (ex: ZipException) {
            return Either.Left(BinaryError.UNABLE_TO_UNPACK_BINARY)
        }

        return Either.Right(Unit)
    }
}