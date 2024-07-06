package org.grakovne.sideload.kindle.shelf.service

import org.apache.commons.lang3.RandomStringUtils
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.shelf.configuration.ShelfWebProperties
import org.grakovne.sideload.kindle.shelf.domain.ShelfContentItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfReference
import org.grakovne.sideload.kindle.shelf.repository.ShelfReferenceRepository
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.util.UUID

@Service
class ShelfService(
    private val shelfItemService: ShelfItemService,
    private val environmentService: UserEnvironmentService,
    private val repository: ShelfReferenceRepository,
    private val shelfWebProperties: ShelfWebProperties
) {

    fun fetchShelfLink(userId: String): String {
        return UriComponentsBuilder
            .fromUriString(shelfWebProperties.hostName)
            .path(fetchOrCreateShelf(userId).shortId)
            .toUriString()
    }

    fun fetchShelfContent(shortId: String): List<ShelfContentItem> {
        val shelf = repository.findByShortId(shortId) ?: return emptyList()

        return shelf
            .id
            .let { shelfItemService.provideShelfItems(it) }
            .flatMap { item ->
                environmentService
                    .provideEnvironmentFiles(item.environmentId)
                    .map {
                        ShelfContentItem(
                            file = it,
                            createdAt = item.createdAt,
                            environmentId = item.environmentId
                        )
                    }
            }
    }

    fun fetchOrCreateShelf(userId: String) = repository
        .findByUserId(userId)
        ?: createShelf(userId)

    private fun createShelf(userId: String): ShelfReference {
        val entity = ShelfReference(
            id = UUID.randomUUID(),
            shortId = RandomStringUtils.randomAlphabetic(5),
            userId = userId
        )

        return repository.save(entity)
    }
}