package org.grakovne.sideload.kindle.common

private val TELEGRAM_BOT_TOKEN = Regex("(?<=file/bot)[0-9][^/@\\s]*")

fun maskTelegramToken(value: Any?): String = value
    ?.toString()
    ?.replace(TELEGRAM_BOT_TOKEN, "***")
    ?: "null"
