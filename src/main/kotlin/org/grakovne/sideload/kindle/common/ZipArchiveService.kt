package org.grakovne.sideload.kindle.common

import net.lingala.zip4j.ZipFile
import org.springframework.stereotype.Service
import java.io.File

@Service
class ZipArchiveService {

    fun unpack(file: File, where: File) {
        return ZipFile(file).extractAll(where.absolutePath)
    }
}