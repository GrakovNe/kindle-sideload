package org.grakovne.sideload.kindle.telegram.localization.domain

open class Button(val payload: String? = null) : Message {

    override fun equals(other: Any?): Boolean = this.javaClass.simpleName == other?.javaClass?.simpleName

    override fun hashCode(): Int = this.javaClass.simpleName.hashCode()
}