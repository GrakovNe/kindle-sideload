package org.grakovne.sideload.kindle.shelf.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

//@Entity
//@Table(name = "shelf_item")
data class ShelfItem(
    @Id
    val id: String,
    val shelfId: String,
    val environmentId: String,
    val createdAt: Instant,
    val status: ShelfItemStatus
)

enum class ShelfItemStatus {
    ACTIVE,
    TERMINATED
}