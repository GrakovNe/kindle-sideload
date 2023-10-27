package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.converter.configuration

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendDocument
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.ButtonPressedEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.springframework.stereotype.Service

@Service
class FetchUserConfigurationEventHandler(
    private val bot: TelegramBot,
    private val userConverterConfigurationService: UserConverterConfigurationService,
    private val messageSender: MessageWithNavigationSender,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventHandler<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(FetchUserConfigurationButton::class.java)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        userConverterConfigurationService
            .fetchConverterConfiguration(event.user.id)
            .map { SendDocument(event.user.id, it).fileName("configuration.zip") }
            .map { bot.execute(it) }
            .also {
                messageSender
                    .sendResponse(
                        event.update,
                        event.user,
                        ConverterUserConfigurationMessage,
                        listOf(
                            listOf(BackToSettingsButton),
                            listOf(MainScreenButton)
                        )
                    )
            }
    }

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)
}