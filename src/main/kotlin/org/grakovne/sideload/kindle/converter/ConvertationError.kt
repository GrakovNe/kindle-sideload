package org.grakovne.sideload.kindle.converter

sealed class ConvertationError(open val details: String?, open val environmentId: String?)

data object UnableFetchFile : ConvertationError(null, null)

data object UnableDeployEnvironment : ConvertationError(null, null)

data class UnableConvertFile(
    override val environmentId: String,
    val reason: String
) : ConvertationError(reason, environmentId)