package org.grakovne.sideload.kindle.shelf.domain

import java.io.File
import java.time.Instant

data class ShelfContentItem (
    val environmentId: String,
    val file: File,
    val createdAt: Instant
)