package org.grakovne.sideload.kindle.stk.email.delivery.domain

/**
 * A delivery-failure cluster key derived from the bounce itself: an Amazon
 * error code ("E001"), an SMTP enhanced status ("5.7.1") or a semantic
 * cluster name for well-known failure families ("malicious-content").
 *
 * Deliberately not a closed enum: an unrecognised code becomes its own
 * cluster, so new Amazon or provider errors show up in the metric without
 * a code change.
 */
@JvmInline
value class FailureReason(val key: String)
