package org.grakovne.sideload.kindle.telegram

import com.pengrad.telegrambot.model.Update
import mu.KotlinLogging
import java.util.UUID

fun Update.fetchUniqueIdentifier(): String {
    if (null != this.message()) {
        return this.message().messageId().toString()
    }

    if (null != this.callbackQuery()) {
        return this.callbackQuery().message().messageId().toString()
    }

    logger.error { "Unable to extract unique message identifier from $this" }
    return UUID.randomUUID().toString()
}

fun Update.fetchUserId(): String {
    if (null != this.myChatMember()?.chat()?.id()) {
        return this.myChatMember().chat().id().toString()
    }

    if (null != this.message()) {
        if (null != this.message().chat().id()) {
            return this.message().chat().id().toString()
        }

        return this.message().from().id().toString()
    }

    if (null != this.callbackQuery()) {
        if (null != this.callbackQuery()?.message()?.chat()?.id()) {
            return this.callbackQuery().message().chat().id().toString()
        }

        return this.callbackQuery().from().id().toString()
    }

    logger.error { "Unable to extract user identifier from $this" }
    throw IllegalArgumentException("Unable to extract user identifier from $this")
}

fun Update.fetchLanguage(): String {
    if (null != this.message()) {
        return this.message().from()?.languageCode() ?: "en"
    }

    if (null != this.callbackQuery()) {
        return this.callbackQuery().from().languageCode() ?: "en"
    }

    return "en"
}

private val logger = KotlinLogging.logger { }