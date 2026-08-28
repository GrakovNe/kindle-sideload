package org.grakovne.sideload.kindle.shelf.domain

import java.util.UUID

data class ShelfReference(
    val id: UUID,
    val shortId: String,
    val userId: String
)
