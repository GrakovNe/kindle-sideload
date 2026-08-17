package org.grakovne.sideload.kindle.telegram.handlers.common

import arrow.core.Either
import kotlinx.coroutines.runBlocking
import org.grakovne.sideload.kindle.events.core.Event
import org.grakovne.sideload.kindle.events.core.EventProcessingError
import org.grakovne.sideload.kindle.events.core.EventProcessingResult
import org.grakovne.sideload.kindle.events.core.EventType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReplyingEventHandlerTest {

    private data object TestType : EventType
    private data object TestError : EventProcessingError

    private class TestEvent : Event(TestType)

    private class TestHandler(
        private val result: Either<TestError, EventProcessingResult>
    ) : ReplyingEventHandler<TestEvent, TestError>() {
        var successCalls = 0
        var failureCalls = 0
        var lastFailure: TestError? = null

        override fun acceptableEvents() = listOf(TestType)

        override suspend fun onEvent(event: TestEvent): Either<TestError, EventProcessingResult> = result

        override fun sendSuccessfulResponse(event: TestEvent) {
            successCalls++
        }

        override fun sendFailureResponse(event: TestEvent, code: TestError) {
            failureCalls++
            lastFailure = code
        }
    }

    @Test
    fun `sends a successful response when the event is processed`() = runBlocking {
        val handler = TestHandler(Either.Right(EventProcessingResult.PROCESSED))

        val result = handler.handleEvent(TestEvent())

        assertEquals(Either.Right(EventProcessingResult.PROCESSED), result)
        assertEquals(1, handler.successCalls)
        assertEquals(0, handler.failureCalls)
    }

    @Test
    fun `does not reply when the event is skipped`() = runBlocking {
        val handler = TestHandler(Either.Right(EventProcessingResult.SKIPPED))

        val result = handler.handleEvent(TestEvent())

        assertEquals(Either.Right(EventProcessingResult.SKIPPED), result)
        assertEquals(0, handler.successCalls)
        assertEquals(0, handler.failureCalls)
    }

    @Test
    fun `sends a failure response with the error when the event fails`() = runBlocking {
        val handler = TestHandler(Either.Left(TestError))

        val result = handler.handleEvent(TestEvent())

        assertEquals(Either.Left(TestError), result)
        assertEquals(1, handler.failureCalls)
        assertEquals(TestError, handler.lastFailure)
        assertEquals(0, handler.successCalls)
    }
}
