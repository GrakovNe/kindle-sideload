package org.grakovne.sideload.kindle.common

fun <T> Boolean.ifTrue(action: () -> T) {
    if (this) {
        action.invoke()
    }
}