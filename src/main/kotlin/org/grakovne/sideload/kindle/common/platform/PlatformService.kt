package org.grakovne.sideload.kindle.common.platform

import arrow.core.Either
import mu.KotlinLogging
import org.apache.commons.lang3.SystemUtils
import org.springframework.stereotype.Service


@Service
class PlatformService {

    fun fetchPlatformName(): Either<PlatformError, String> {
        logger.debug { "Try to find host OS and architecture" }

        if (SystemUtils.IS_OS_WINDOWS) {
            logger.warn { "Found host OS and architecture. Working on Windows and it's not supported" }
            return Either.Left(PlatformError.WINDOWS_PLATFORM_IS_NOT_SUPPORTED)
        }

        if (System.getProperty("sun.arch.data.model") == "32" && SystemUtils.IS_OS_LINUX) {
            logger.debug { "Found host OS and architecture. Working on 32-Bit Linux" }
            return Either.Right("linux-i386")
        }

        if (System.getProperty("sun.arch.data.model") != "64") {
            logger.warn { "Found host OS and architecture. Working on 32-Bit non-Linux and it's not supported" }
            return Either.Left(PlatformError.ONLY_64_BIT_PLATFORM_ARE_SUPPORTED_EXCEPT_LINUX)
        }

        if (SystemUtils.IS_OS_MAC) {
            logger.debug { "Found host OS and architecture. Working on 64-Bit MacOS" }
            return Either.Right("darwin-arm64")
        }

        if (SystemUtils.IS_OS_LINUX) {
            val architecture = System.getProperty("os.arch")
            logger.debug { "Found host OS and architecture. Working on 64-Bit Linux" }

            return when {
                architecture.contains("arm") -> Either.Right("linux-arm64")
                architecture.contains("aarch64") -> Either.Right("linux-arm64")
                architecture.contains("amd64") -> Either.Right("linux-amd64")
                architecture.contains("x86_64") -> Either.Right("linux-amd64")
                else -> Either.Left(PlatformError.UNABLE_TO_DEFINE_LINUX_PLATFORM)
                    .also { logger.warn { "Linux detected but arch not supported. Your arch is: $architecture" } }
            }
        }

        return Either.Left(PlatformError.UNABLE_TO_DEFINE_PLATFORM)
            .also {
                logger.error {
                    """Unable to find host OS and architecture. 
                        |Details: 
                        |   Architecture: ${System.getProperty("sun.arch.data.model")}
                        |   OS: ${System.getProperty("os.name")}
                    """
                        .trimMargin()
                }
            }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}