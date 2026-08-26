package org.grakovne.sideload.kindle.common

import com.ibm.icu.text.Transliterator
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Service
class FileDownloadService(
    private val restTemplate: RestTemplate
) {

    suspend fun download(link: String, fileName: String? = null): File? = withRetry { tryDownload(link, fileName) }

    private fun tryDownload(link: String, fileName: String?): File? = restTemplate.execute(
        link,
        HttpMethod.GET,
        null,
        {
            val name = link.substringAfterLast("/")

            val file = when (fileName) {
                null -> File.createTempFile(RandomStringUtils.randomAlphabetic(3), "_$name")
                else -> createSafeTempFile(fileName)
            }

            logger.debug { "Created empty temporary file: ${file.absoluteFile}" }

            StreamUtils.copy(it.body, FileOutputStream(file))
            logger.debug { "Content from $link successfully downloaded to ${file.absoluteFile}" }

            file
        }
    )

    private suspend fun <T> withRetry(operation: () -> T?): T? {
        var lastException: Exception? = null

        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                logger.debug { "Attempt ${attempt + 1} of $MAX_RETRY_ATTEMPTS to download file" }
                return operation()
            } catch (ex: Exception) {
                lastException = ex
                logger.warn { "Attempt ${attempt + 1} of $MAX_RETRY_ATTEMPTS failed due to: ${ex.message}" }

                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }

        logger.warn { "All $MAX_RETRY_ATTEMPTS attempts failed to download from remote. Last exception: ${lastException?.message}" }
        return null
    }

    fun createSafeTempFile(originalName: String?): File {
        val name = originalName?.trim().orEmpty().ifEmpty { "file" }

        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot).lowercase(Locale.ROOT) else ""

        val safeName = Transliterator
            .getInstance("Any-Latin; Latin-ASCII")
            .transliterate(base)
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
            .ifEmpty { "file" } + ext
            .replace("[^a-z0-9.]".toRegex(), "")
            .replace("\\.+".toRegex(), ".")

        return File(System.getProperty("java.io.tmpdir"), safeName)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
}
