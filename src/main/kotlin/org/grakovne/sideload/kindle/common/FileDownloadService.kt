package org.grakovne.sideload.kindle.common

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

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
                val file = File.createTempFile(UUID.randomUUID().toString(), ".file")
                StreamUtils.copy(it.body, FileOutputStream(file))
                file
            }
        )
}