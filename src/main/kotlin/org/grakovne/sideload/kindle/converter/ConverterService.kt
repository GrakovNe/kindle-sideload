package org.grakovne.sideload.kindle.converter

import arrow.core.Either
import org.springframework.stereotype.Service
import java.io.File

@Service
class ConverterService(
    private val converter: Fb2ConverterService,
    private val bypass: EpubBypassConverterService
) {

    fun convertAndCollect(
        userId: String,
        book: File
    ): Either<ConvertationError, ConversionResult> =
        when (book.extension.lowercase()) {
            "epub" -> bypass.convertAndCollect(userId, book)
            else -> converter.convertAndCollect(userId, book)
        }
}
