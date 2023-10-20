package org.grakovne.sideload.kindle.environment

import org.grakovne.sideload.kindle.telegram.domain.error.EventProcessingError

interface EnvironmentError : EventProcessingError

data object UnableDeployError : EnvironmentError
data object UnableTerminateError : EnvironmentError