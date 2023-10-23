package org.grakovne.sideload.kindle.telegram

import com.pengrad.telegrambot.model.Update

fun Update.fetchUniqueIdentifier(): String {
    if (null != this.message()) {
        return this.message().messageId().toString()
    }

    if (null != this.callbackQuery()) {
        return this.callbackQuery().message().messageId().toString()
    }

    throw IllegalArgumentException("Change me later")
}

fun Update.fetchUserId(): String {
    if (null != this.message()) {
        return this.message().from().id().toString()
    }

    if (null != this.callbackQuery()) {
        return this.callbackQuery().from().id().toString()
    }

    throw IllegalArgumentException("Change me later")
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