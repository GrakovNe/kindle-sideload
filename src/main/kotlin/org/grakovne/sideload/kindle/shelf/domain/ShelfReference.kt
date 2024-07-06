package org.grakovne.sideload.kindle.shelf.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

//@Entity
//@Table(name = "shelf_reference")
data class ShelfReference(
    @Id
    val id: UUID,
    val shortId: String,
    val userId: String
)