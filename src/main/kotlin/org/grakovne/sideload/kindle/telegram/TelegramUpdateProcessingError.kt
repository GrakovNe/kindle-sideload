package org.grakovne.sideload.kindle.telegram

enum class TelegramUpdateProcessingError {
    RESPONSE_NOT_SENT,
    LOCALIZATION_ERROR,
    INTERNAL_ERROR,
    TARGET_USER_DISAPPEAR
}