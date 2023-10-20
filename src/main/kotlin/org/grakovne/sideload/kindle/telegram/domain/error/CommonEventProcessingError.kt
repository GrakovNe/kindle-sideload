package org.grakovne.sideload.kindle.telegram.domain.error

import org.grakovne.sideload.kindle.events.core.EventProcessingError

data object UnableSendResponse : EventProcessingError
data object LocalizationError : EventProcessingError
data object UnknownError : EventProcessingError