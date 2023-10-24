package org.grakovne.sideload.kindle.transferring.email.domain

import org.grakovne.sideload.kindle.events.core.EventProcessingError

interface TransferEmailError: EventProcessingError

object InternalError: TransferEmailError