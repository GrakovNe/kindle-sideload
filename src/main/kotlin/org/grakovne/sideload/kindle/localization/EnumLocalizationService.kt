package org.grakovne.sideload.kindle.localization

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.grakovne.sideload.kindle.common.Language
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.io.InputStream

@Service
class EnumLocalizationService(val objectMapper: ObjectMapper) {

    fun localize(enum: Enum<*>, language: String?) = findLocalizationResources(language)
        .find { it.name == enum::class.simpleName }
        ?.values
        ?.toList()
        ?.find { (value, _) -> value == enum.name }
        ?.second
        ?: enum.name

    private fun findLocalizationResources(language: Language?): List<EnumTemplate> {
        val content = getLocalizationResource(language)
            .readBytes()

        return objectMapper.readValue(content, object : TypeReference<List<EnumTemplate>>() {})
    }

    private fun getLocalizationResource(language: Language?): InputStream {
        if (language == null) {
            return ClassPathResource("enums.json").inputStream
        }

        return try {
            ClassPathResource("enums_${language}.json").inputStream
        } catch (ex: FileNotFoundException) {
            ClassPathResource("enums.json").inputStream
        }
    }
}