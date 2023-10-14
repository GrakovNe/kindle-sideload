package org.grakovne.sideload.kindle.user.repository

import org.grakovne.sideload.kindle.user.domain.Type
import org.grakovne.sideload.kindle.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String> {

    fun findByType(type: Type): List<User>
}