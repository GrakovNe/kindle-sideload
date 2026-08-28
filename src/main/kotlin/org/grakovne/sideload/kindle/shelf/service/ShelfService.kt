package org.grakovne.sideload.kindle.shelf.service

import org.apache.commons.lang3.RandomStringUtils
import org.grakovne.sideload.kindle.environment.UserEnvironmentService
import org.grakovne.sideload.kindle.shelf.configuration.ShelfWebProperties
import org.grakovne.sideload.kindle.shelf.domain.ShelfContentItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfReference
import org.grakovne.sideload.kindle.shelf.repository.ShelfReferenceDao
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.util.UUID

@Service
class ShelfService(
    private val shelfItemService: ShelfItemService,
    private val environmentService: UserEnvironmentService,
    private val repository: ShelfReferenceDao,
    private val shelfWebProperties: ShelfWebProperties
) {

    fun fetchShelfLink(userId: String): String {
        return UriComponentsBuilder
            .fromUriString(shelfWebProperties.hostName)
            .path(fetchOrCreateShelf(userId).shortId)
            .toUriString()
    }

    fun fetchUserId(shortId: String) = repository.findByShortId(shortId)?.userId

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

    fun fetchOrCreateShelf(userId: String): ShelfReference = repository
        .findByUserId(userId)
        ?: createShelf(userId)

    /**
     * The short id is unique in the database, so uniqueness is decided by the insert rather than
     * by a preceding lookup — a check-then-insert would still let two concurrent callers pick the
     * same id. A rejected insert simply means the id was taken, so retry with a new one.
     */
    private fun createShelf(userId: String): ShelfReference {
        repeat(SHORT_ID_ATTEMPTS) {
            val reference = ShelfReference(
                id = UUID.randomUUID(),
                shortId = RandomStringUtils.randomAlphabetic(SHORT_ID_LENGTH),
                userId = userId
            )

            repository.saveIfAbsent(reference)?.let { return it }
        }

        throw IllegalStateException(
            "Unable to allocate a free short shelf id for user $userId in $SHORT_ID_ATTEMPTS attempts"
        )
    }

    companion object {
        private const val SHORT_ID_LENGTH = 5
        private const val SHORT_ID_ATTEMPTS = 10
    }
}
