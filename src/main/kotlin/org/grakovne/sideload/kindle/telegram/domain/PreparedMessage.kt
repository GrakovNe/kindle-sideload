package org.grakovne.sideload.kindle.telegram.domain

data class PreparedMessage(
    val text: String,
    val enablePreview: Boolean
) : PreparedItem()