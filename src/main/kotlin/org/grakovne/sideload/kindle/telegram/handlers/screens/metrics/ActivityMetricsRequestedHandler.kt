package org.grakovne.sideload.kindle.telegram.handlers.screens.metrics

import arrow.core.Either
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.metrics.service.ActivityMetricService
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestMetricsButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.main.RequestProjectInfoButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class ActivityMetricsRequestedHandler(
    private val messageSender: MessageWithNavigationSender,
    private val activityMetricService: ActivityMetricService,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventHandler<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(RequestMetricsButton::class.java)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        val snapshot = activityMetricService.aggregateMetrics()

        messageSender
            .sendResponse(
                event.update,
                event.user,
                ActivityMetricsMessage(
                    fileConvertationsToday = snapshot.fileConvertations.today,
                    fileConvertationsWeekly = snapshot.fileConvertations.weekly,
                    fileConvertationsYearly = snapshot.fileConvertations.yearly,

                    usersToday = snapshot.users.today,
                    usersWeekly = snapshot.users.today,
                    usersYearly = snapshot.users.today,
                ),
                listOf(
                    listOf(MainScreenButton)
                )
            )

    }

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)

}