package org.grakovne.sideload.kindle.telegram.localization

import arrow.core.Either
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.Language
import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.telegram.domain.PreparedItem
import org.grakovne.sideload.kindle.telegram.localization.converter.toMessage
import org.grakovne.sideload.kindle.telegram.localization.template.TextTemplate
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.time.Instant
import kotlin.io.path.Path
import kotlin.reflect.full.memberProperties

abstract class LocalizationService<T : Message, R : PreparedItem, F : TextTemplate>(
    private val resourceName: String,
    private val enumLocalizationService: EnumLocalizationService
) {

    private fun findOverrideTemplate(language: Language?, name: String): F? {
        val file = getLocalizedResourceFile(language)

        if (!file.exists()) {
            return null
        }

        val content = try {
            deserializeResourceContent(file.inputStream().use { it.readBytes() })
        } catch (e: Exception) {
            logger.error(e) { "Unable to parse the localized resource file ${file.name}. Falling back to the default classpath templates" }
            return null
        }

        return content.find { it.name == name }
    }

    private fun findDefaultTemplate(name: String): F? =
        ClassPathResource("$resourceName.json").inputStream.use { it.readBytes() }
            .let { deserializeResourceContent(it) }
            .find { it.name == name }

    abstract fun deserializeResourceContent(content: ByteArray): List<F>

    abstract fun applyLocalization(
        language: Language?,
        localizedValues: Map<String, String>,
        template: F
    ): Either<LocalizationError, R>

    fun localize(message: T, language: Language?): Either<LocalizationError, R> {
        val templateName = message.javaClass.simpleName
        logger.info { "Localize $templateName with $language language" }

        val localizationTemplate: F? = findOverrideTemplate(language, templateName)
            ?: findDefaultTemplate(templateName).also { fallback ->
                if (fallback != null && getLocalizedResourceFile(language).exists()) {
                    logger.warn { "The template $templateName is missing from the localized resource file. Falling back to the default classpath template" }
                }
            }

        if (localizationTemplate == null) {
            return Either
                .Left(LocalizationError.TEMPLATE_NOT_FOUND)
                .also { logger.error { "Unable to find acceptable template for message $message in both the localized and the default resources. Skipping responding" } }
        }

        val values = message::class
            .memberProperties
            .mapNotNull { member -> message.getField(member.name, language)?.let { member.name to it } }
            .toMap()

        return applyLocalization(language, values, localizationTemplate)
    }

    private fun getLocalizedResourceFile(language: Language?): File {
        val resourceFile = language
            ?.let { "${resourceName}_$language.json" }
            ?: "$resourceName.json"

        return Path("locale").resolve(resourceFile).toFile()
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
