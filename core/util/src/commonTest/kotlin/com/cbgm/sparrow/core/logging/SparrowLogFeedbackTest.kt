package com.cbgm.sparrow.core.logging

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class SparrowLogFeedbackTest {
    @Test
    fun repeatedErrorsRemainQueuedBeforeTheUiCollects() = runBlocking {
        SparrowLog.error("FeedbackQueueTest", "first")
        SparrowLog.error("FeedbackQueueTest", "first")
        SparrowLog.error("FeedbackQueueTest", "second")
        assertEquals(
            listOf("FeedbackQueueTest: first", "FeedbackQueueTest: first", "FeedbackQueueTest: second"),
            withTimeout(5_000.milliseconds) { SparrowLog.errors.take(3).toList() }
        )
    }

    @Test
    fun loggedErrorsReachTheDeveloperLogEvenWhenSinkInstallsLater() = runBlocking {
        val saved = CompletableDeferred<String>()
        SparrowLog.error("LateLogTest", "before sink installation")
        SparrowLog.installErrorSink { tag, _, message, _ ->
            if (tag == "LateLogTest") saved.complete(message)
        }
        assertEquals("before sink installation", withTimeout(5_000.milliseconds) { saved.await() })
    }

    @Test
    fun hintsStaySeparateFromErrors() = runBlocking {
        SparrowLog.hint("hint one")
        SparrowLog.hint("hint two")
        assertEquals(listOf("hint one", "hint two"), withTimeout(5_000.milliseconds) { SparrowLog.hints.take(2).toList() })
    }
}
