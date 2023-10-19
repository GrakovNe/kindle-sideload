package org.grakovne.sideload.kindle.converter

sealed class ConvertationError(open val details: String?, open val environmentId: String?)

data object UnableFetchFile : ConvertationError(null, null)

data object UnableDeployEnvironment : ConvertationError(null, null)

data class FatalError(
    override val details: String
) : ConvertationError(details, null)

data class UnableConvertFile(
    override val details: String,
    override val environmentId: String,
) : ConvertationError(details, environmentId)