package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.time.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Pays for a new mailbox: finds a nonce so that `SHA-256("sparrow-mailbox-work-v1|stamp|createdAt|nonce")` starts
 * with [bits] zero bits, as the mailbox service checks. About 2^[bits] hashes, under a second on a phone.
 */
internal class MailboxCreationWorkSolver(
    private val hash: CryptoHash,
    private val bits: Int = REQUIRED_BITS,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    suspend fun solve(): MailboxCreationWork =
        withContext(Dispatchers.Default) {
            val stamp = IdGenerator.generate()
            val createdAt = now()
            var nonce = 0L
            while (leadingZeroBits(hash.sha256("$CONTEXT|$stamp|$createdAt|$nonce".encodeToByteArray())) < bits) {
                nonce++
                if (nonce % CANCELLATION_CHECK_INTERVAL == 0L) ensureActive()
            }
            MailboxCreationWork(stamp = stamp, createdAtEpochMilliseconds = createdAt, nonce = nonce)
        }

    private fun leadingZeroBits(digest: ByteArray): Int {
        var zeroBits = 0
        for (byte in digest) {
            val value = byte.toInt() and 0xFF
            if (value != 0) return zeroBits + value.countLeadingZeroBits() - (Int.SIZE_BITS - Byte.SIZE_BITS)
            zeroBits += Byte.SIZE_BITS
        }
        return zeroBits
    }

    companion object {
        /** What the mailbox service asks for by default (`MAILBOX_CREATION_WORK_BITS`). */
        const val REQUIRED_BITS = 18
        private const val CONTEXT = "sparrow-mailbox-work-v1"
        private const val CANCELLATION_CHECK_INTERVAL = 4_096L
    }
}
