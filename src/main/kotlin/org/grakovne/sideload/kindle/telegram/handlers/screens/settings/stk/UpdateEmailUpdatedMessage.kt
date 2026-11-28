package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.stk

import org.grakovne.sideload.kindle.common.navigation.domain.Message
import org.grakovne.sideload.kindle.user.preferences.service.validation.UpdateEmailValidationError

data object AutoStkScreenMessage : Message
data object StkScreenMessage : Message
data object UpdateEmailPromptMessage : Message

/**
 * [senderEmail] is the configured `spring.mail.from` rather than a literal in the template: the
 * address users have to whitelist must never be able to drift from the one we send from.
 */
data class UpdateEmailUpdatedMessage(val senderEmail: String) : Message

data class UpdateEmailUpdateFailedMessage(val reason: UpdateEmailValidationError) : Message
