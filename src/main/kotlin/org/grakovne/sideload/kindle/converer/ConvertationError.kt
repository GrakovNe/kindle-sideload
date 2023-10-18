package org.grakovne.sideload.kindle.converer

sealed class ConvertationError(open val details: String?)

data object UnableFetchFile: ConvertationError(null)
data object UnableDeployEnvironment: ConvertationError(null)
data class UnableConvertFile(val reason: String): ConvertationError(reason)