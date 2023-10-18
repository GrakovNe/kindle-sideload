package org.grakovne.sideload.kindle.platform

import arrow.core.Either
import mu.KotlinLogging
import org.apache.commons.lang3.SystemUtils
import org.springframework.stereotype.Service


@Service
class PlatformService {

    fun fetchPlatformName(): Either<PlatformError, String> {
        if (SystemUtils.IS_OS_WINDOWS) {
            return Either.Left(PlatformError.WINDOWS_PLATFORM_IS_NOT_SUPPORTED)
        }

        if (System.getProperty("sun.arch.data.model") == "32" && SystemUtils.IS_OS_LINUX) {
            return Either.Right("linux_i386")
        }

        if (System.getProperty("sun.arch.data.model") != "64") {
            return Either.Left(PlatformError.ONLY_64_BIT_PLATFORM_ARE_SUPPORTED_EXCEPT_LINUX)
        }

        if (SystemUtils.IS_OS_MAC) {
            return Either.Right("darwin_arm64")
        }

        if (SystemUtils.IS_OS_LINUX) {
            val architecture = System.getProperty("os.arch")

            return when {
                architecture.contains("arm") -> Either.Right("linux_arm64")
                architecture.contains("amd64") -> Either.Right("linux_amd64")
                architecture.contains("x86_64") -> Either.Right("linux_amd64")
                else -> Either.Left(PlatformError.UNABLE_TO_DEFINE_LINUX_PLATFORM)
            }
        }

        return Either.Left(PlatformError.UNABLE_TO_DEFINE_PLATFORM)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}