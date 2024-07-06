package org.grakovne.sideload.kindle.shelf.common

import org.grakovne.sideload.kindle.events.core.EventProcessingError

sealed interface ShelfProcessingError : EventProcessingError

data object UnableAttachItemError : ShelfProcessingError