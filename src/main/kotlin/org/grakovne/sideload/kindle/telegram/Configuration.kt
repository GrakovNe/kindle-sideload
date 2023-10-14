package org.grakovne.sideload.kindle.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.TelegramBot.Builder
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class Configuration(private val properties: ConfigurationProperties) {

    @Bean
    fun telegramBotService(): TelegramBot = Builder(properties.token).build()
}