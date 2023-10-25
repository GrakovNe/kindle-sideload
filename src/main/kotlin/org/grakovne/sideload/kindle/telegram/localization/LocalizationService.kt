package org.grakovne.sideload.kindle.telegram.localization

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.Language
import org.grakovne.sideload.kindle.telegram.domain.PreparedItem
import org.grakovne.sideload.kindle.telegram.localization.converter.toMessage
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.telegram.localization.template.TextTemplate
import org.springframework.core.io.ClassPathResource
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Instant
import kotlin.reflect.full.memberProperties

abstract class LocalizationService<T : Message, R : PreparedItem, F : TextTemplate>(
    private val resourceName: String,
    private val enumLocalizationService: EnumLocalizationService
) {

    private fun findLocalizationResources(language: Language?) = getLocalizationResource(language)
        .readBytes()
        .let { deserializeResourceContent(it) }

    abstract fun deserializeResourceContent(content: ByteArray): List<F>

    abstract fun applyLocalization(
        language: Language?,
        localizedValues: Map<String, String>,
        template: F
    ): Either<LocalizationError, R>

    fun localize(message: T, language: Language?): Either<LocalizationError, R> {
        logger.info { "Localize $message with $language language" }

        val localizationTemplate: F = findLocalizationResources(language)
            .find { it.name == message.javaClass.simpleName }
            ?.also { logger.debug { "Found acceptable template for message $message: ${it.name}" } }
            ?: return Either
                .Left(LocalizationError.TEMPLATE_NOT_FOUND)
                .also { logger.error { "Unable to find acceptable template for message $message. Skipping responding" } }

        val values = message::class
            .memberProperties
            .mapNotNull { member -> message.getField(member.name, language)?.let { member.name to it } }
            .toMap()

        return applyLocalization(language, values, localizationTemplate)
    }

    private fun getLocalizationResource(language: Language?): InputStream {
        if (null == language) {
            return ClassPathResource("$resourceName.json").inputStream
        }

        return try {
            ClassPathResource("${resourceName}_${language}.json").inputStream
        } catch (ex: FileNotFoundException) {
            ClassPathResource("$resourceName.json").inputStream
        }
    }

    private fun Any.getField(fieldName: String, language: Language?): String? {
        this::class.memberProperties.forEach { kCallable ->
            if (fieldName == kCallable.name) {
                return when (val rawValue = kCallable.getter.call(this)) {
                    is Enum<*> -> enumLocalizationService.localize(rawValue, language)
                    is Instant -> rawValue.toMessage()
                    else -> rawValue.toString()
                }
            }
        }

        return null
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}