package org.grakovne.sideload.kindle.metrics.api.domain

data class DailyMetrics(
    val convertedBooks: Int,
    val failedBooks: Int,
    val sentEmails: Int,
    val failedEmails: Int,
    val users: List<UserDailyMetrics>
)

data class UserDailyMetrics(
    val userId: String,
    val sentMessages: Int
)
