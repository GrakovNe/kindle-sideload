package org.grakovne.sideload.kindle.telegram.listeners.screens.convertation

import org.grakovne.sideload.kindle.telegram.localization.domain.Button
import java.io.File

class SendConvertedToEmailButton(
    val files: List<File> = emptyList()
) : Button(mapOf("files" to files.map { it.absoluteFile }))