package org.grakovne.sideload.kindle.common

fun Boolean.ifTrue(action: () -> Any) {
    if (this) {
        action.invoke()
    }
}