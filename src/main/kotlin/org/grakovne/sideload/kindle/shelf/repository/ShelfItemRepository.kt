package org.grakovne.sideload.kindle.shelf.repository

import org.grakovne.sideload.kindle.shelf.domain.ShelfItem
import org.grakovne.sideload.kindle.shelf.domain.ShelfItemStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ShelfItemRepository : JpaRepository<ShelfItem, UUID> {

    fun findByShelfIdAndStatus(userId: UUID, status: ShelfItemStatus): List<ShelfItem>
    fun findByEnvironmentId(environmentId: String): ShelfItem?
}