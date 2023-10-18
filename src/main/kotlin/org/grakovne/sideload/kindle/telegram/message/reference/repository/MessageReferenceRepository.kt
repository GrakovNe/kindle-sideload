package org.grakovne.sideload.kindle.telegram.message.reference.repository

import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageReference
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MessageReferenceRepository : JpaRepository<MessageReference, String>