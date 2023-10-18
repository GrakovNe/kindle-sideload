package org.grakovne.sideload.kindle.events.core

abstract class Event(val eventType: EventType)

enum class EventType {
    CONVERTATION_FINISHED,
    LOG_SENT,
    INCOMING_MESSAGE,
    ENVIRONMENT_UNNECESSARY
}


