package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.stk

import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.user.preferences.service.validation.UpdateEmailValidationError

data object AutoStkScreenMessage : Message
data object StkScreenMessage : Message
data object UpdateEmailPromptMessage : Message
data object UpdateEmailUpdatedMessage : Message
data class UpdateEmailUpdateFailedMessage(val reason: UpdateEmailValidationError) : Message