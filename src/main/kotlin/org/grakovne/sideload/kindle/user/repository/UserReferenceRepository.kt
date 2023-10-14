package org.grakovne.sideload.kindle.user.repository

import org.grakovne.sideload.kindle.user.domain.Type
import org.grakovne.sideload.kindle.user.domain.UserReference
import org.springframework.data.jpa.repository.JpaRepository

interface UserReferenceRepository : JpaRepository<UserReference, String> {

    fun findByType(type: Type): List<UserReference>
}