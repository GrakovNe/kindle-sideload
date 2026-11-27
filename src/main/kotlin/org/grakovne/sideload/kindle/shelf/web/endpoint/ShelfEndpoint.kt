package org.grakovne.sideload.kindle.shelf.web.endpoint

import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.shelf.converter.ShelfContentItemConverter
import org.grakovne.sideload.kindle.shelf.converter.toFileName
import org.grakovne.sideload.kindle.shelf.service.ShelfService
import org.grakovne.sideload.kindle.shelf.web.localization.LocalizedTemplateProvider
import org.grakovne.sideload.kindle.user.reference.service.UserService
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ofPattern

@Controller
class ShelfEndpoint(
    private val shelfService: ShelfService,
    private val shelfContentItemConverter: ShelfContentItemConverter,
    private val environmentService: UserEnvironmentService,
    private val userService: UserService,
    private val localizedTemplateProvider: LocalizedTemplateProvider
) {

    @GetMapping("/download/{environmentId}/{fileUrl}")
    fun downloadBinary(
        @PathVariable environmentId: String,
        @PathVariable fileUrl: String,
    ): ResponseEntity<FileSystemResource> {
        // the file system listing order is not stable, so pick deterministically: an environment can
        // hold two files whose sanitized names collide, and the Kindle browser must not alternate
        // between them across requests
        val file = environmentService
            .provideEnvironmentFiles(environmentId)
            .sortedBy { it.name }
            .firstOrNull { it.name.toFileName() == fileUrl }
            ?: return ResponseEntity.notFound().build()

        val resource = FileSystemResource(file)

        val headers = HttpHeaders().apply {
            add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileUrl}\"")
            add(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
        }

        return ResponseEntity(resource, headers, HttpStatus.OK)
    }

    @GetMapping("/{shortUserId}")
    fun index(
        @PathVariable shortUserId: String,
        model: Model
    ): String {
        val files = shelfService
            .fetchShelfContent(shortUserId)
            .sortedByDescending { it.createdAt }
            .map { shelfContentItemConverter.apply(it) }

        val userLanguage = shelfService
            .fetchUserId(shortUserId)
            ?.let { userService.fetchUser(it) }
            ?.language ?: "en"

        model.addAttribute("files", files)
        model.addAttribute("currentDate", LocalDate.now().format(ofPattern("dd.MM.yyyy")))
        model.addAttribute("currentTime", LocalTime.now().format(ofPattern("HH:mm")))

        return localizedTemplateProvider.provideLocalized("shelf", userLanguage)
    }
}
