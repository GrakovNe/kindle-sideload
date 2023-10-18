package org.grakovne.sideload.kindle.localization

import arrow.core.Either
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.commons.text.StringSubstitutor
import org.grakovne.sideload.kindle.common.Language
import org.grakovne.sideload.kindle.localization.adverisement.AdvertisingService
import org.grakovne.sideload.kindle.localization.converter.toMessage
import org.grakovne.sideload.kindle.telegram.domain.PreparedMessage
import org.grakovne.swiftbot.localization.MessageTemplate
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Instant
import kotlin.reflect.full.memberProperties


@Service
class MessageLocalizationService(
    private val objectMapper: ObjectMapper,
    private val enumLocalizationService: EnumLocalizationService,
    private val advertisingService: AdvertisingService
) {

    fun localize(message: Message, language: Language?): Either<LocalizationError, PreparedMessage> {
        val messageTemplate: MessageTemplate = findLocalizationResources(language)
            .find { it.name == message.templateName }
            ?: return Either.Left(LocalizationError.TEMPLATE_NOT_FOUND)

        val values = message::class
            .memberProperties
            .mapNotNull { member -> message.getField(member.name, language)?.let { member.name to it } }
            .toMap()

        return StringSubstitutor(values)
            .replace(messageTemplate.template)
            .let { it + advertisingService.provideContent(messageTemplate, language) }
            .let { PreparedMessage(it, messageTemplate.enablePreview.not()) }
            .let { Either.Right(it) }
    }

    private fun findLocalizationResources(language: Language?): List<MessageTemplate> {
        val content = getLocalizationResource(language)
            .readBytes()

        return objectMapper.readValue(content, object : TypeReference<List<MessageTemplate>>() {})
    }

    private fun getLocalizationResource(language: Language?): InputStream {
        if (null == language) {
            return ClassPathResource("messages.json").inputStream
        }

        return try {
            ClassPathResource("messages_${language}.json").inputStream
        } catch (ex: FileNotFoundException) {
            ClassPathResource("messages.json").inputStream
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