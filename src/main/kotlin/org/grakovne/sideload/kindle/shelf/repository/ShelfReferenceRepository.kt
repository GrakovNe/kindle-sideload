package org.grakovne.sideload.kindle.shelf.repository

import org.grakovne.sideload.kindle.shelf.domain.ShelfReference
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ShelfReferenceRepository : JpaRepository<ShelfReference, UUID> {

    fun findByUserId(userId: String): ShelfReference?
    fun findByShortId(shortId: String): ShelfReference?
}