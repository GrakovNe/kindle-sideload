package org.grakovne.sideload.kindle.telegram.handlers.screens.settings.converter.configuration.upload

import arrow.core.Either
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetFile
import mu.KotlinLogging
import org.grakovne.sideload.kindle.common.FileDownloadService
import org.grakovne.sideload.kindle.common.configuration.FileUploadProperties
import org.grakovne.sideload.kindle.common.navigation.ButtonService
import org.grakovne.sideload.kindle.common.navigation.domain.Button
import org.grakovne.sideload.kindle.telegram.domain.ButtonPressedEvent
import org.grakovne.sideload.kindle.telegram.handlers.common.InputRequiredEventHandler
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.BackToSettingsButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.MainScreenButton
import org.grakovne.sideload.kindle.telegram.handlers.screens.settings.converter.configuration.UploadConfigurationButton
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationSubmittedMessage
import org.grakovne.sideload.kindle.telegram.navigation.UserConfigurationValidationFailedMessage
import org.grakovne.sideload.kindle.telegram.sender.MessageWithNavigationSender
import org.grakovne.sideload.kindle.telegram.state.service.UserActivityStateService
import org.grakovne.sideload.kindle.user.configuration.UserConverterConfigurationService
import org.grakovne.sideload.kindle.user.configuration.domain.FileAbsentError
import org.grakovne.sideload.kindle.user.configuration.domain.FileIsTooLargeError
import org.grakovne.sideload.kindle.user.configuration.domain.InternalError
import org.grakovne.sideload.kindle.user.configuration.domain.UserConverterConfigurationError
import org.grakovne.sideload.kindle.user.configuration.domain.ValidationError
import org.springframework.stereotype.Service

@Service
class UserConfigurationUploadSubmitHandler(
    private val bot: TelegramBot,
    private val fileDownloadService: FileDownloadService,
    private val userConverterConfigurationService: UserConverterConfigurationService,
    private val properties: FileUploadProperties,
    private val messageSender: MessageWithNavigationSender,
    buttonService: ButtonService,
    userActivityStateService: UserActivityStateService,
) : InputRequiredEventHandler<UserConverterConfigurationError>(userActivityStateService, buttonService) {

    override fun getRequiredButton(): List<Button> = listOf(UploadConfigurationButton)

    override fun sendSuccessfulResponse(event: ButtonPressedEvent) {
        messageSender.sendResponse(
            origin = event.update,
            user = event.user,
            message = UserConfigurationSubmittedMessage,
            listOf(
                listOf(BackToSettingsButton),
                listOf(MainScreenButton)
            )
        )
    }

    override fun sendFailureResponse(event: ButtonPressedEvent, code: UserConverterConfigurationError) {
        when (code) {
            is ValidationError -> messageSender.sendResponse(
                origin = event.update,
                user = event.user,
                message = UserConfigurationValidationFailedMessage(code.code)
            )
        }
    }

    override fun processEvent(event: ButtonPressedEvent): Either<UserConverterConfigurationError, Unit> {
        val file = event
            .update
            .message()
            ?.document()
            ?: return Either.Left(FileAbsentError)

        if (file.fileSize() > properties.maxSize) {
            return Either.Left(FileIsTooLargeError)
        }

        val configurationFile = bot
            .execute(GetFile(file.fileId()))
            .file()
            .let { bot.getFullFilePath(it) }
            .let { fileDownloadService.download(it) }
            ?: return Either.Left(InternalError)

        return userConverterConfigurationService
            .updateConverterConfiguration(event.user, configurationFile)
            .map { }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}