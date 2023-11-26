package org.grakovne.sideload.kindle.metrics.domain

data class ActivityMetrics(
    val fileConvertations: PeriodicMetrics,
    val users: PeriodicMetrics
)

data class PeriodicMetrics(
    val today: Int,
    val weekly: Int,
    val yearly: Int
)