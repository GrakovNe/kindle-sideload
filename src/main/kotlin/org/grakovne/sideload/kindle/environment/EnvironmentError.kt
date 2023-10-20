package org.grakovne.sideload.kindle.environment

import org.grakovne.sideload.kindle.telegram.domain.error.NewEventProcessingError

interface EnvironmentError : NewEventProcessingError

data object UnableDeployError : EnvironmentError
data object UnableTerminateError : EnvironmentError