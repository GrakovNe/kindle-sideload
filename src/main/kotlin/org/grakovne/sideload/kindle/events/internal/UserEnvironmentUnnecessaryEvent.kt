package org.grakovne.sideload.kindle.events.internal

import org.grakovne.sideload.kindle.events.core.EnvironmentUnnecessary
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventType

data class UserEnvironmentUnnecessaryEvent(val environmentId: String?) : Event(EnvironmentUnnecessary)