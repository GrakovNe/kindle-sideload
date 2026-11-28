package org.grakovne.sideload.kindle.notification.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "notifications")
class NotificationProperties {
    var enabled: Boolean = true

    /**
     * How long one campaign is stretched over. The enqueue step divides it by the audience size
     * and stamps every notification with its own slot, so the campaign duration is the knob and
     * the message rate is only ever its consequence.
     */
    var spreadOver: Duration = Duration.ofHours(72)

    /**
     * Hard ceiling per worker tick. The schedule alone already paces the campaign; this exists so
     * a mistyped `spread-over` cannot turn into a burst.
     */
    var maxPerTick: Int = 3

    var maxAttempts: Int = 3
}
