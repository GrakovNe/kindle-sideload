package org.grakovne.sideload.kindle.telegram.message.reference.service

import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageReference
import org.grakovne.sideload.kindle.telegram.message.reference.domain.MessageStatus
import org.grakovne.sideload.kindle.telegram.message.reference.repository.MessageReferenceRepository
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class MessageReferenceService(
    private val repository: MessageReferenceRepository
) {

    fun fetchMessage(id: String): MessageReference? = repository.findById(id).getOrNull()

    fun markAsProcessed(id: String) = MessageReference(id = id, status = MessageStatus.PROCESSED)
        .let { repository.save(it) }
}