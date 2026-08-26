package org.grakovne.sideload.kindle.events.core

import arrow.core.Either
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EventSenderTest {

    private data object TestType : EventType
    private data object OtherType : EventType

    private class TestEvent : Event(TestType)
    private class OtherEvent : Event(OtherType)

    private data object TestError : EventProcessingError

    private class FakeHandler(
        private val types: List<EventType>,
        private val result: Either<TestError, EventProcessingResult>
    ) : EventHandler<TestEvent, TestError>() {
        var calls = 0

        override fun acceptableEvents() = types

        override suspend fun onEvent(event: TestEvent): Either<TestError, EventProcessingResult> {
            calls++
            return result
        }
    }

    @Test
    fun `sends the event only to the handlers that accept its type`() {
        val accepting = FakeHandler(listOf(TestType, OtherType), Either.Right(EventProcessingResult.PROCESSED))
        val other = FakeHandler(listOf(OtherType), Either.Right(EventProcessingResult.PROCESSED))
        val sender = EventSender(listOf(accepting, other))

        val results = sender.sendEvent(TestEvent())

        assertEquals(listOf(Either.Right(EventProcessingResult.PROCESSED)), results)
        assertEquals(1, accepting.calls)
        assertEquals(0, other.calls)
    }

    @Test
    fun `returns no results when no handler accepts the event`() {
        val other = FakeHandler(listOf(OtherType), Either.Right(EventProcessingResult.PROCESSED))
        val sender = EventSender(listOf(other))

        val results = sender.sendEvent(TestEvent())

        assertEquals(emptyList(), results)
        assertEquals(0, other.calls)
    }

    @Test
    fun `collects both successful and failing handler results in order`() {
        val success = FakeHandler(listOf(TestType), Either.Right(EventProcessingResult.PROCESSED))
        val failure = FakeHandler(listOf(TestType), Either.Left(TestError))
        val sender = EventSender(listOf(success, failure))

        val results = sender.sendEvent(TestEvent())

        assertEquals(
            listOf(
                Either.Right(EventProcessingResult.PROCESSED),
                Either.Left(TestError)
            ),
            results
        )
    }

    @Test
    fun `an empty listener list produces an empty result`() {
        val sender = EventSender(emptyList())

        assertEquals(emptyList(), sender.sendEvent(TestEvent()))
    }
}
