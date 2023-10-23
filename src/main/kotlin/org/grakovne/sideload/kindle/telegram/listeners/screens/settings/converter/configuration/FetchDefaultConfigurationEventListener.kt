package org.grakovne.sideload.kindle.telegram.listeners.screens.settings.converter.configuration

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendDocument
import org.grakovne.sideload.kindle.assets.configuration.default.DefaultConfigurationAssetService
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.listeners.ButtonPressedEventListener
import org.grakovne.sideload.kindle.telegram.listeners.screens.main.MainScreenRequestedMessage
import org.grakovne.sideload.kindle.telegram.listeners.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.messaging.NavigatedMessageSender
import org.grakovne.sideload.kindle.telegram.navigation.ButtonService
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.springframework.stereotype.Service

@Service
class FetchDefaultConfigurationEventListener(
    private val bot: TelegramBot,
    private val defaultConfigurationAssetService: DefaultConfigurationAssetService,
    private val messageSender: NavigatedMessageSender,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : ButtonPressedEventListener<EventProcessingError>(buttonService, userActivityStateService) {

    override fun getOperatingButtons() = listOf(FetchDefaultConfigurationButton)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        defaultConfigurationAssetService
            .fetchDefaultConfiguration()
            .let { SendDocument(event.user.id, it).fileName("configuration.zip") }
            .let { bot.execute(it) }
            .also {
                messageSender
                    .sendResponse(
                        event.update,
                        event.user,
                        MainScreenRequestedMessage,
                        listOf(
                            listOf(FetchDefaultConfigurationButton),
                            listOf(BackToSettingsButton),
                        )
                    )
            }
    }

    override fun processEvent(event: ButtonPressedEvent): Either<EventProcessingError, Unit> = Either.Right(Unit)
}