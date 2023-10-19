package org.grakovne.sideload.kindle.telegram.message.reference.repository

import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageReference
import org.springframework.data.jpa.repository.JpaRepository

interface MessageReferenceRepository : JpaRepository<MessageReference, String>