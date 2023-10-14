package org.grakovne.sideload.kindle.events.core

abstract class Event(val eventType: EventType)

enum class EventType {
    LOG_SENT
}


