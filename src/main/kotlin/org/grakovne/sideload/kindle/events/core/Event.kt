package org.grakovne.sideload.kindle.events.core

abstract class Event(val eventType: EventType)

interface EventType

data object StkFinished : EventType
data object ConvertationFinished : EventType
data object IncomingMessage : EventType
data object EnvironmentUnnecessary : EventType

