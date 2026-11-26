package org.grakovne.sideload.kindle.delivery.domain

enum class DeliveryFailureReason {
    UNSUPPORTED_FORMAT,
    NO_ATTACHMENT,
    THROTTLED,
    REJECTED_AS_MALICIOUS,
    UNKNOWN_RECIPIENT,
    BOUNCED,
    UNKNOWN,
}
