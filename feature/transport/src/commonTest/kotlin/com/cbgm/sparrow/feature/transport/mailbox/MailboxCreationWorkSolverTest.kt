package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.crypto.hash.DefaultCryptoHash
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MailboxCreationWorkSolverTest {
    private val hash = DefaultCryptoHash()

    @Test
    fun theSolvedWorkStartsWithTheAskedZeroBits() =
        runTest {
            val work = MailboxCreationWorkSolver(hash, bits = 12, now = { 1_234L }).solve()

            val digest = hash.sha256("sparrow-mailbox-work-v1|${work.stamp}|1234|${work.nonce}".encodeToByteArray())
            assertEquals(0, digest[0].toInt())
            assertEquals(0, digest[1].toInt() and 0xF0)
            assertEquals(1_234L, work.createdAtEpochMilliseconds)
        }

    @Test
    fun everyMailboxGetsItsOwnStamp() =
        runTest {
            val solver = MailboxCreationWorkSolver(hash, bits = 4)

            assertNotEquals(solver.solve().stamp, solver.solve().stamp)
            assertTrue(solver.solve().stamp.length in 16..64)
        }
}
