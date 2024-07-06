package org.grakovne.sideload.kindle.shelf.web.endpoint

import org.grakovne.sideload.kindle.shelf.converter.ShelfContentItemConverter
import org.grakovne.sideload.kindle.shelf.service.ShelfService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ofPattern

@Controller
class ShelfEndpoint(
    private val shelfService: ShelfService,
    private val shelfContentItemConverter: ShelfContentItemConverter
) {

    @RequestMapping("/{shortUserId}")
    fun index(
        @PathVariable shortUserId: String,
        model: Model
    ): String {

        val files = shelfService
            .fetchShelfContent(shortUserId)
            .map { shelfContentItemConverter.apply(it) }

        model.addAttribute("files", files)
        model.addAttribute("currentDate", LocalDate.now().format(ofPattern("dd.MM.yyyy")))
        model.addAttribute("currentTime", LocalTime.now().format(ofPattern("HH:mm")))

        return "shelf"
    }
}