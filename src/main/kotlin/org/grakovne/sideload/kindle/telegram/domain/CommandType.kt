package org.grakovne.sideload.kindle.telegram.domain

enum class CommandType {
    MAIN_SCREEN_REQUESTED,
    SETTINGS_SCREEN_REQUESTED,
    UPLOAD_CONFIGURATION_REQUEST,
    REMOVE_CONFIGURATION_REQUEST
}