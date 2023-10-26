package org.grakovne.sideload.kindle.telegram.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
class AsyncLoggerConfig(
    loggerAppender: AsyncLoggerAppender
) {

    init {
        val context: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerAppender.context = context
        loggerAppender.start()

        val rootLogger: Logger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.addAppender(loggerAppender)
    }
}