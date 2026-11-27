package org.grakovne.sideload.kindle.shelf.web.endpoint

import org.grakovne.sideload.kindle.shelf.web.localization.LocalizedTemplateProvider
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ofPattern
import java.util.Locale

@Controller
class LandingEndpoint(
    private val localizedTemplateProvider: LocalizedTemplateProvider
) {

    // Without this mapping "/" matches nothing and the browser is left with the reverse proxy's
    // default page, which reads as a broken deployment rather than as "you need the bot".
    @GetMapping("/")
    fun landing(
        locale: Locale,
        model: Model
    ): String {
        model.addAttribute("currentDate", LocalDate.now().format(ofPattern("dd.MM.yyyy")))
        model.addAttribute("currentTime", LocalTime.now().format(ofPattern("HH:mm")))

        return localizedTemplateProvider.provideLocalized("root", locale.language)
    }
}
