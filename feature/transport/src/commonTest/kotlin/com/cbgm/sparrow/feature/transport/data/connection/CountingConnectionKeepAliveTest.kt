package com.cbgm.sparrow.feature.transport.data.connection

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CountingConnectionKeepAliveTest {
    @Test
    fun theConnectionIsHeldUntilEveryHolderLetGo() =
        runTest {
            val keepAlive = CountingConnectionKeepAlive()
            keepAlive.hold()
            keepAlive.hold()
            keepAlive.release()

            assertTrue(keepAlive.observeHeld().first())

            keepAlive.release()
            keepAlive.release()
            assertFalse(keepAlive.observeHeld().first())

            keepAlive.hold()
            assertTrue(keepAlive.observeHeld().first())
        }
}
