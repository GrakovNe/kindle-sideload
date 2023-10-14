package org.grakovne.sideload.kindle

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KindleSideloadApplication

fun main(args: Array<String>) {
	runApplication<KindleSideloadApplication>(*args)
}
