package org.grakovne.sideload.kindle.transferring.email.task.domain

import org.grakovne.sideload.kindle.events.core.EventProcessingError

interface TransferEmailError : EventProcessingError

object InternalError : TransferEmailError
object UserEmailAbsent : TransferEmailError
object SendingError : TransferEmailError