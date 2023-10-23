package org.grakovne.sideload.kindle.telegram.localization.domain

open class Button(val data: Map<String, Any> = emptyMap()) : Message {

    override fun equals(other: Any?) = this.javaClass.simpleName == other?.javaClass?.simpleName

    override fun hashCode() = this.javaClass.simpleName.hashCode()
}