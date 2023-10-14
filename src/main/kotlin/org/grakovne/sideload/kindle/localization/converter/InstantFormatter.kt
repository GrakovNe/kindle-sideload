package org.grakovne.sideload.kindle.localization.converter

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd.MM.yyyy HH:mm:ss")
    .withZone(ZoneId.of("UTC"));

fun Instant.toMessage(): String = dateFormatter.format(this)