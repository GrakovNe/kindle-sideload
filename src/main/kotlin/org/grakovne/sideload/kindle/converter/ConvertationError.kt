package org.grakovne.sideload.kindle.converter

sealed class ConvertationError(open val details: String?, open val environmentId: String?)

data object UnableFetchFile : ConvertationError(null, null)

data object UnableDeployEnvironment : ConvertationError(null, null)

data class UnableConvertFile(
    val reason: String,
    override val environmentId: String,
) : ConvertationError(reason, environmentId)