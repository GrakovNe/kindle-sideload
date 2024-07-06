package org.grakovne.sideload.kindle.shelf.converter

import com.ibm.icu.text.Transliterator.getInstance
import org.apache.commons.text.StringEscapeUtils

private val transliterator = getInstance("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC")

fun String.toFileName() = transliterator
    .transliterate(this)
    .let { StringEscapeUtils.escapeHtml4(it) }
    .replace(" ", "_")
    .filter { it.code in 32..126 }