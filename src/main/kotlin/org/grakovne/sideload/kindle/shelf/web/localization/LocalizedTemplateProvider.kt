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
    ): String = when {
        null == language -> template
        templateExists(template, language) -> "${template}_$language"
        else -> template
    }

    private fun templateExists(
        resourceName: String,
        language: Language
    ): Boolean {
        return Path(TEMPLATES)
            .resolve(Path("${resourceName}_$language.$TEMPLATE_EXTENSION"))
            .let { ClassPathResource(it.toString()) }
            .exists()
    }

    companion object {
        private const val TEMPLATES = "templates"
        private const val TEMPLATE_EXTENSION = "html"
    }
}
