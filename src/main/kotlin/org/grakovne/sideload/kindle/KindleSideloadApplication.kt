package org.grakovne.sideload.kindle

import org.grakovne.sideload.kindle.mail.MailSendingService
import org.grakovne.sideload.kindle.user.common.OutputFormat
import org.grakovne.sideload.kindle.user.preferences.domain.UserPreferences
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import java.io.File
import java.util.UUID

//@EnableScheduling
@SpringBootApplication
class KindleSideloadApplication {
    @Autowired
    lateinit var mailSendingService: MailSendingService

    @Bean
    fun cli() = CommandLineRunner { _ ->

        val up = UserPreferences(
            id = UUID.randomUUID(),
            userId = "",
            email = "grakovne@gmail.com",
            outputFormat = OutputFormat.EPUB,
            debugMode = true
        )

        val string = "Hello!"

        val file = File.createTempFile(UUID.randomUUID().toString(), ".txt")
        file.writeBytes(string.toByteArray())

        mailSendingService.sendFile(up, files = listOf(file))
    }
}

fun main(args: Array<String>) {
    runApplication<KindleSideloadApplication>(*args)
}
