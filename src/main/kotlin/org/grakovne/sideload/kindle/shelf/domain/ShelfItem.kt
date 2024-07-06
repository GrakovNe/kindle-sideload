package org.grakovne.sideload.kindle.shelf.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "shelf_item")
data class ShelfItem(
    @Id
    val id: String,
    val shelfId: UUID,
    val environmentId: String,
    val createdAt: Instant,

    @Enumerated(EnumType.STRING)
    val status: ShelfItemStatus
)

enum class ShelfItemStatus {
    ACTIVE,
    TERMINATED
}