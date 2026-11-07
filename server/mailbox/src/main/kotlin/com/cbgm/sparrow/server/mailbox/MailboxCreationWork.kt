package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.protocol.MailboxCreationWork
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * The price of a new mailbox: instead of counting mailboxes per address (behind a circuit, every user of a session
 * node shares one), the app shows a small proof of work. `SHA-256("sparrow-mailbox-work-v1|stamp|createdAt|nonce")`
 * must start with [requiredBits] zero bits. A stamp counts once and only for [windowMilliseconds] around its time;
 * used stamps are kept only that long, and nothing about the creator.
 */
internal class MailboxCreationWorkCheck(
    private val requiredBits: Int,
    private val windowMilliseconds: Long = DEFAULT_WINDOW_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val usedStamps = ConcurrentHashMap<String, Long>()

    init {
        require(requiredBits in 0..MAXIMUM_BITS) { "Mailbox creation work must be 0 to $MAXIMUM_BITS bits" }
    }

    fun accepts(work: MailboxCreationWork?): Boolean {
        val at = now()
        val valid =
            work != null &&
                work.stamp.length in STAMP_LENGTHS &&
                abs(at - work.createdAtEpochMilliseconds) <= windowMilliseconds &&
                leadingZeroBits(digest(work)) >= requiredBits
        if (!valid) return false
        usedStamps.entries.removeIf { (_, expiresAt) -> expiresAt < at }
        return usedStamps.putIfAbsent(work.stamp, work.createdAtEpochMilliseconds + windowMilliseconds) == null
    }

    private fun digest(work: MailboxCreationWork): ByteArray =
        MessageDigest
            .getInstance("SHA-256")
            .digest("$CONTEXT|${work.stamp}|${work.createdAtEpochMilliseconds}|${work.nonce}".encodeToByteArray())

    private fun leadingZeroBits(digest: ByteArray): Int {
        var bits = 0
        for (byte in digest) {
            val value = byte.toInt() and 0xFF
            if (value != 0) return bits + Integer.numberOfLeadingZeros(value) - (Int.SIZE_BITS - Byte.SIZE_BITS)
            bits += Byte.SIZE_BITS
        }
        return bits
    }

    private companion object {
        const val CONTEXT = "sparrow-mailbox-work-v1"
        const val MAXIMUM_BITS = 32
        const val DEFAULT_WINDOW_MILLISECONDS = 10L * 60L * 1_000L
        val STAMP_LENGTHS = 16..64
    }
}
