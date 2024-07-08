package org.grakovne.sideload.kindle.shelf.web.localization

import org.grakovne.sideload.kindle.common.Language
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import kotlin.io.path.Path

@Service
class LocalizedTemplateProvider {

    fun provideLocalized(
        template: String,
        language: Language?
    ): String = when (templateExists(template, language)) {
        true -> "${template}_${language}"
        false -> template
    }

    private fun templateExists(
        resourceName: String,
        language: Language?
    ): Boolean {
        val localizedResourceName = language
            ?.let { "${resourceName}_${language}" }
            ?: resourceName

        return Path(TEMPLATES)
            .resolve(Path("$localizedResourceName.$TEMPLATE_EXTENSION"))
            .let { ClassPathResource(it.toString()) }
            .exists()
    }

    companion object {
        private const val TEMPLATES = "templates"
        private const val TEMPLATE_EXTENSION = "html"
    }
}