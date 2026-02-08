package org.grakovne.sideload.kindle.common

import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream

@Service
class FileDownloadService(
    private val restTemplate: RestTemplate
) {

    suspend fun download(link: String): File? = withRetry(link) { tryDownload(link) }

    private fun tryDownload(link: String): File? = restTemplate.execute(
        link,
        HttpMethod.GET,
        null,
        {
            val fileName = link.substringAfterLast("/")
            val file = File.createTempFile(RandomStringUtils.randomAlphabetic(3), "_$fileName")
            logger.debug { "Created empty temporary file: ${file.absoluteFile}" }

            StreamUtils.copy(it.body, FileOutputStream(file))
            logger.debug { "Content from $link successfully downloaded to ${file.absoluteFile}" }

            file
        }
    )

    private suspend fun <T> withRetry(link: String, operation: () -> T?): T? {
        var lastException: Exception? = null

        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                logger.debug { "Attempt ${attempt + 1} of $MAX_RETRY_ATTEMPTS to download from $link" }
                return operation()
            } catch (ex: Exception) {
                lastException = ex
                logger.warn { "Attempt ${attempt + 1} of $MAX_RETRY_ATTEMPTS failed for $link: ${ex.message}" }

                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }

        logger.warn { "All $MAX_RETRY_ATTEMPTS attempts failed to download from $link. Last exception: ${lastException?.message}" }
        return null
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
}