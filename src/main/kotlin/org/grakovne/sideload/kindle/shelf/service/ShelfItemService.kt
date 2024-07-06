package org.grakovne.sideload.kindle.shelf.service

import arrow.core.Either
import org.grakovne.sideload.kindle.shelf.common.ShelfItemError
import org.grakovne.sideload.kindle.shelf.domain.ShelfItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItemStatus
import org.grakovne.sideload.kindle.shelf.repository.ShelfItemRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ShelfItemService(
    private val repository: ShelfItemRepository
) {

    fun attachToShelf(
        shelfId: UUID,
        environmentId: String,
    ): Either<ShelfItemError, Unit> = repository
        .findByEnvironmentId(environmentId)
        ?.let { Either.Left(ShelfItemError.ITEM_ALREADY_EXISTS) }
        ?: createItem(shelfId, environmentId)

    fun provideShelfItems(
        shelfId: UUID
    ): List<ShelfItem> = repository.findByShelfIdAndStatus(shelfId, ShelfItemStatus.ACTIVE)

    private fun createItem(
        shelfId: UUID,
        environmentId: String,
    ): Either<ShelfItemError, Unit> {
        val entity = ShelfItem(
            id = UUID.randomUUID(),
            shelfId = shelfId,
            environmentId = environmentId,
            createdAt = Instant.now(),
            status = ShelfItemStatus.ACTIVE
        )

        return repository.save(entity).let { Either.Right(Unit) }
    }
}