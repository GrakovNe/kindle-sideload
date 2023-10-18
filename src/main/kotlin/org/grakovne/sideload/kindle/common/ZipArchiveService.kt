package org.grakovne.sideload.kindle.common

import mu.KotlinLogging
import net.lingala.zip4j.ZipFile
import org.springframework.stereotype.Service
import java.io.File

@Service
class ZipArchiveService {

    fun unpack(file: File, where: File) = ZipFile(file).extractAll(where.absolutePath)

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}