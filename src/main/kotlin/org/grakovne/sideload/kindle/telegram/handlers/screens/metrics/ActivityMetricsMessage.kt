package org.grakovne.sideload.kindle.telegram.handlers.screens.metrics

import org.grakovne.sideload.kindle.common.navigation.domain.Message

data class ActivityMetricsMessage(
    val fileConvertationsToday: Int,
    val fileConvertationsWeekly: Int,
    val fileConvertationsYearly: Int,

    val usersToday: Int,
    val usersWeekly: Int,
    val usersYearly: Int
): Message