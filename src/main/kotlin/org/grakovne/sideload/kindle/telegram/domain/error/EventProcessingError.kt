package org.grakovne.sideload.kindle.telegram.domain.error

interface EventProcessingError

data object UnableSendResponse: EventProcessingError
data object LocalizationError: EventProcessingError
data object UnknownError: EventProcessingError