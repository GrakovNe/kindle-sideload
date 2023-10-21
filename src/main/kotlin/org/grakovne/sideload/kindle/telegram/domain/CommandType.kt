package org.grakovne.sideload.kindle.telegram.domain

enum class CommandType {
    CONVERT_BOOK_REQUESTED,
    MAIN_SCREEN_REQUESTED,
    UPLOAD_CONFIGURATION_REQUEST,
    REMOVE_CONFIGURATION_REQUEST
}