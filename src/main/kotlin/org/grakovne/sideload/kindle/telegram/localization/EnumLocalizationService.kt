package org.grakovne.sideload.kindle.telegram.localization

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.grakovne.sideload.kindle.common.Language
import org.grakovne.sideload.kindle.telegram.localization.template.EnumTemplate
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.InputStream
import kotlin.io.path.Path

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
        val resourceName = language
            ?.let { "enums_${language}.json" }
            ?: "enums.json"

        return Path("locale").resolve(Path(resourceName)).toFile().let {
            when (it.exists()) {
                true -> it.inputStream()
                false -> ClassPathResource("enums_.json").inputStream
            }
        }
    }
}