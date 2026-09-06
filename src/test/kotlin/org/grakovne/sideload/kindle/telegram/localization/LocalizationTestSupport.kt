package org.grakovne.sideload.kindle.telegram.localization

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

internal fun kotlinMapper(): JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .build()
