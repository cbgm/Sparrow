package com.cbgm.sparrow.server.gateway

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GatewaySessionWorkDispatcherTest {
    @Test
    fun differentRecipientsDoNotBlockEachOther() =
        runTest {
            val dispatcher = GatewaySessionWorkDispatcher()
            val firstStarted = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()
            val secondCompleted = CompletableDeferred<Unit>()

            try {
                dispatcher.dispatch("envelope:recipient-a") {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                }
                dispatcher.dispatch("envelope:recipient-b") {
                    secondCompleted.complete(Unit)
                }

                firstStarted.await()
                withRealTimeout {
                    secondCompleted.await()
                }
            } finally {
                releaseFirst.complete(Unit)
                dispatcher.close()
            }
        }

    @Test
    fun sameRecipientKeepsFrameOrder() =
        runTest {
            val dispatcher = GatewaySessionWorkDispatcher()
            val events = mutableListOf<String>()
            val firstStarted = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()
            val secondCompleted = CompletableDeferred<Unit>()

            try {
                dispatcher.dispatch("indicator:recipient-a") {
                    events += "first-start"
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                    events += "first-end"
                }
                dispatcher.dispatch("indicator:recipient-a") {
                    events += "second"
                    secondCompleted.complete(Unit)
                }

                firstStarted.await()
                assertEquals(listOf("first-start"), events)

                releaseFirst.complete(Unit)
                withRealTimeout {
                    secondCompleted.await()
                }

                assertEquals(
                    listOf("first-start", "first-end", "second"),
                    events
                )
            } finally {
                releaseFirst.complete(Unit)
                dispatcher.close()
            }
        }
}
