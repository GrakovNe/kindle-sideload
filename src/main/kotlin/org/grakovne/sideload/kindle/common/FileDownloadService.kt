package org.grakovne.sideload.kindle.common

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

    fun download(link: String) = restTemplate
        .execute(
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

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}