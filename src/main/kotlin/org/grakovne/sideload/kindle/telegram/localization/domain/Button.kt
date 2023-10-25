package org.grakovne.sideload.kindle.telegram.localization.domain

open class Button(
    val payload: String? = null
) : Message {

    val name: String = javaClass.simpleName

    override fun equals(other: Any?): Boolean = this.javaClass.simpleName == other?.javaClass?.simpleName

    override fun hashCode(): Int = this.javaClass.simpleName.hashCode()

    companion object {
        fun Button.buildQualifiedName() = if (this.payload == null) this.name else this.name + "#" + this.payload

        fun fetchButtonName(raw: String) = raw.split(buttonPayloadDelimiter).first()

        fun fetchButtonPayload(raw: String) = raw.split(buttonPayloadDelimiter).last()

        private val buttonPayloadDelimiter = "#"
    }
}