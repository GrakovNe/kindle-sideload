package org.grakovne.sideload.kindle.converter.binary.unpack

import arrow.core.Either
import mu.KotlinLogging
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.grakovne.sideload.kindle.converter.binary.provider.ConverterBinaryProvider
import org.grakovne.sideload.kindle.converter.binary.reference.domain.BinaryError
import org.springframework.stereotype.Service
import java.io.File


@Service
class ArchivedBinaryUnpackService(
    private val binaryProvider: ConverterBinaryProvider
) {

    fun unpack(archive: File): Either<BinaryError, Unit> {
        logger.info { "Unpacking a converter Binary from the archive" }

        try {
            ZipFile(archive).extractAll(binaryProvider.provideBinaryFolder().absolutePath)
            logger.info { "Unpacked a converter Binary from the archive" }
        } catch (ex: ZipException) {
            logger.error { "Unable to unpack a converter Binary from the archive. See details: $ex" }
            return Either.Left(BinaryError.UNABLE_TO_UNPACK_BINARY)
        }

        return Either.Right(Unit)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}