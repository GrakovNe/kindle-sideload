package org.grakovne.sideload.kindle.shelf.converter

import com.ibm.icu.text.Transliterator.getInstance
import org.apache.commons.text.StringEscapeUtils

private val transliterator = getInstance("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC")

fun String.toFileName(): String {
    val transliterated = transliterator.transliterate(this)
    val escaped = StringEscapeUtils.escapeHtml4(transliterated)
    return escaped
        .replace(" ", "_")
        .filter { it.code in 32..126 }
}