package org.grakovne.sideload.kindle.telegram.domain

enum class CommandType {
    SEND_HELP,
    MAIN_MENU_REQUESTED,
    UPLOAD_CONFIGURATION_REQUEST,
    REMOVE_CONFIGURATION_REQUEST
}