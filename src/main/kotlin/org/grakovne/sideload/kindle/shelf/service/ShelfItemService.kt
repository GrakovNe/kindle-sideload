package org.grakovne.sideload.kindle.shelf.service

import org.grakovne.sideload.kindle.shelf.domain.ShelfItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItemStatus
import org.grakovne.sideload.kindle.shelf.repository.ShelfItemRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ShelfItemService(
    private val repository: ShelfItemRepository
) {

    fun provideShelfItems(
        shelfId: UUID
    ): List<ShelfItem> = repository.findByShelfIdAndStatus(shelfId, ShelfItemStatus.ACTIVE)
}