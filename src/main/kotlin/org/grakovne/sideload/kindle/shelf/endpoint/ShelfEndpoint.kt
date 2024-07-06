package org.grakovne.sideload.kindle.shelf.endpoint

import org.grakovne.sideload.kindle.shelf.endpoint.view.ShelfItemView
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@Controller
class ShelfEndpoint {

    @RequestMapping
    fun index(model: Model): String {
        val files: List<ShelfItemView> = listOf(
            ShelfItemView("Book Title 1.epub", "/file1.epub"),
            ShelfItemView("Book Title 2.mobi", "/file2.mobi"),
            ShelfItemView("Book Title 3.pdf", "/file3.pdf"),
            ShelfItemView("Book Title 4.azw3", "/file4.azw3")
        )

        model.addAttribute("files", files)
        model.addAttribute("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
        model.addAttribute("currentTime", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))

        return "shelf"
    }
}