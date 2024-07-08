package org.grakovne.sideload.kindle.shelf.converter

import com.ibm.icu.text.Transliterator.getInstance

private val transliterator = getInstance("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC")

fun String.toFileName() = transliterator
    .transliterate(this)
    .replace(Regex("[^a-zA-Z0-9.]"), "_")