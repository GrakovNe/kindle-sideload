package org.grakovne.sideload.kindle.events.internal

import org.grakovne.sideload.kindle.environment.EnvironmentUnnecessary
import org.grakovne.sideload.kindle.events.core.Event

data class UserEnvironmentUnnecessaryEvent(val environmentId: String?) : Event(EnvironmentUnnecessary)