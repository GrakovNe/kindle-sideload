package org.grakovne.sideload.kindle.environment

import org.grakovne.sideload.kindle.events.core.EventProcessingError

interface EnvironmentError : EventProcessingError

data object UnableDeployError : EnvironmentError
data object UnableTerminateError : EnvironmentError