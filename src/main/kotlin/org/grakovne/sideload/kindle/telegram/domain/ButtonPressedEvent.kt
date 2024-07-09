package org.grakovne.sideload.kindle.telegram.domain

import com.pengrad.telegrambot.model.Update
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventType
import org.grakovne.sideload.kindle.events.core.IncomingMessage
import org.grakovne.sideload.kindle.user.reference.domain.User

data class ButtonPressedEvent(
    val update: Update,
    val user: User
) : Event(IncomingMessage)