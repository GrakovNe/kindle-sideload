package org.grakovne.sideload.kindle.notification.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class NotificationClockConfiguration {

    /** Injectable clock so campaign scheduling can be tested without waiting in real time. */
    @Bean
    fun notificationClock(): Clock = Clock.systemUTC()
}
