package com.cbgm.sparrow.feature.transport.background

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionKeepAliveTest {
    @Test
    fun theConnectionIsHeldUntilEveryHolderLetGo() =
        runTest {
            val keepAlive = ConnectionKeepAlive()
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
