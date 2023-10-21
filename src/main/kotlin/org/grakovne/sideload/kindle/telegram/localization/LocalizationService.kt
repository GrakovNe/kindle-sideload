package org.grakovne.sideload.kindle.telegram.localization

import arrow.core.Either
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.commons.text.StringSubstitutor
import org.grakovne.sideload.kindle.common.Language
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.sideload.kindle.telegram.localization.adverisement.AdvertisingService
import org.grakovne.sideload.kindle.telegram.localization.converter.toMessage
import org.springframework.core.io.ClassPathResource
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Instant
import kotlin.reflect.full.memberProperties

abstract class LocalizationService<T: Message>(
    private val resourceName: String,
    private val enumLocalizationService: EnumLocalizationService,
    private val advertisingService: AdvertisingService,
    private val objectMapper: ObjectMapper,
) {

    private fun findLocalizationResources(language: Language?): List<LocalizationTemplate> {
        val content = getLocalizationResource(language)
            .readBytes()

        return objectMapper.readValue(content, object : TypeReference<List<LocalizationTemplate>>() {})
    }

    fun localize(message: T, language: Language?): Either<LocalizationError, PreparedMessage> {
        logger.info { "Localize $message with $language language" }

        val localizationTemplate: LocalizationTemplate = findLocalizationResources(language)
            .find { it.name == message.template }
            ?.also { logger.debug { "Found acceptable template for message $message: ${it.name}" } }
            ?: return Either
                .Left(LocalizationError.TEMPLATE_NOT_FOUND)
                .also { logger.error { "Unable to find acceptable template for message $message. Skipping responding" } }

        val values = message::class
            .memberProperties
            .mapNotNull { member -> message.getField(member.name, language)?.let { member.name to it } }
            .toMap()

        return StringSubstitutor(values)
            .replace(localizationTemplate.template)
            .let { it + advertisingService.provideContent(localizationTemplate, language) }
            .let { PreparedMessage(it, localizationTemplate.enablePreview.not()) }
            .let { Either.Right(it) }
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
                val rawValue = kCallable.getter.call(this)

                return when (rawValue) {
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