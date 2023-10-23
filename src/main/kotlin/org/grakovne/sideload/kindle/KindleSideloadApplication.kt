package org.grakovne.sideload.kindle

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class KindleSideloadApplication

fun main(args: Array<String>) {
    runApplication<KindleSideloadApplication>(*args)
}
