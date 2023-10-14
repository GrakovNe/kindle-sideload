package org.grakovne.sideload.kindle.events.core

class EventProcessingError<T>(
    val code: T,
    val details: String? = null
)