package org.grakovne.sideload.kindle.telegram.handlers.screens.convertation

import org.grakovne.sideload.kindle.common.navigation.domain.Button

data class SendConvertedToEmailButton(val environmentId: String? = null) : Button(environmentId)