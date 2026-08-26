package org.grakovne.sideload.kindle.shelf.domain

import java.time.Instant
import java.util.UUID

data class ShelfItem(
    val id: UUID,
    val shelfId: UUID,
    val environmentId: String,
    val createdAt: Instant,
    val status: ShelfItemStatus
)

enum class ShelfItemStatus {
    ACTIVE,
    TERMINATED
}
