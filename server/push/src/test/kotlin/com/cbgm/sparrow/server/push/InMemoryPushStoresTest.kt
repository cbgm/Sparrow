package com.cbgm.sparrow.server.push

import kotlinx.coroutines.test.runTest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryPushStoresTest {
    @Test
    fun registeringExistingTokenMovesItToLatestRoutingId() =
        runTest {
            val store = InMemoryPushDeviceStore()

            store.register(PushDevice("routing-1", "token", "ANDROID"))
            store.register(PushDevice("routing-2", "token", "ANDROID"))

            assertEquals(emptyList(), store.find("routing-1"))
            assertEquals(
                listOf(PushDevice("routing-2", "token", "ANDROID")),
                store.find("routing-2")
            )
        }

    @Test
    fun expiredDeviceIsRemovedAndNotFoundForWakeUps() =
        runTest {
            var currentTime = 1_000L
            val store =
                InMemoryPushDeviceStore(
                    lifetimeMilliseconds = 100L,
                    now = { currentTime }
                )

            store.register(PushDevice("routing-1", "token", "ANDROID"))
            currentTime += 100L

            // Wake-ups (FCM sender and node wake-up route) only reach devices returned by find().
            assertEquals(emptyList(), store.find("routing-1"))
            assertEquals(0, store.count())
        }

    @Test
    fun registeringAgainRenewsTheExpiry() =
        runTest {
            var currentTime = 1_000L
            val store =
                InMemoryPushDeviceStore(
                    lifetimeMilliseconds = 100L,
                    now = { currentTime }
                )
            val device = PushDevice("routing-1", "token", "ANDROID")

            store.register(device)
            currentTime += 80L
            store.register(device)
            currentTime += 80L

            assertEquals(listOf(device), store.find("routing-1"))
        }

    @Test
    fun wakeUpIdIsRandomAndAtLeast128Bits() =
        runTest {
            val store = InMemoryWakeUpStore()

            val first = store.create("recipient")
            val second = store.create("recipient")

            assertTrue(Base64.getUrlDecoder().decode(first).size >= 16)
            assertNotEquals(first, second)
            assertNull(store.resolve("$first-guess"))
        }

    @Test
    fun expiredWakeUpCannotBeResolved() =
        runTest {
            var currentTime = 1_000L
            val store =
                InMemoryWakeUpStore(
                    lifetimeMilliseconds = 100L,
                    now = { currentTime }
                )

            val wakeUpId = store.create("recipient")

            assertEquals("recipient", store.resolve(wakeUpId))

            currentTime += 101L

            assertNull(store.resolve(wakeUpId))
        }
}
