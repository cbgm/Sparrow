package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.protocol.CreateMailboxRequest
import com.cbgm.sparrow.server.protocol.MailboxCreationWork
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.CreationWorkCheck
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.security.MessageDigest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxCreationWorkTest {
    @Test
    fun aMailboxNeedsAProofOfWork() =
        mailboxes {
            assertEquals(HttpStatusCode.Forbidden, create(work = null).status)
            assertEquals(HttpStatusCode.Created, create(work = solvedWork(BITS)).status)
        }

    @Test
    fun manyMailboxesFromOneAddressAreFineWhenEachIsPaidFor() =
        mailboxes {
            repeat(5) { assertEquals(HttpStatusCode.Created, create(work = solvedWork(BITS)).status) }
        }

    @Test
    fun aStampCountsOnce() =
        mailboxes {
            val work = solvedWork(BITS)

            assertEquals(HttpStatusCode.Created, create(work).status)
            assertEquals(HttpStatusCode.Forbidden, create(work).status)
        }

    @Test
    fun tooLittleOrStaleWorkIsRejected() {
        val now = 1_000_000_000L
        val check = CreationWorkCheck(requiredBits = BITS, windowMilliseconds = 60_000L, now = { now })

        assertFalse(check.accepts(solvedWork(0, createdAt = now).takeUnless { work -> leadingZeroBits(work) >= BITS }))
        assertFalse(check.accepts(solvedWork(BITS, createdAt = now - 120_000L)))
        assertTrue(check.accepts(solvedWork(BITS, createdAt = now - 30_000L)))
    }

    private fun mailboxes(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                mailboxModule(
                    config =
                        MailboxConfig(
                            databaseUrl = null,
                            databaseUser = "",
                            databasePassword = "",
                            databaseMaximumPoolSize = 1,
                            maximumEnvelopeBytes = 1_048_576,
                            maximumMailboxBytes = 100L * 1_048_576L,
                            creationWorkBits = BITS
                        ),
                    store = MailboxStore()
                )
            }
            block()
        }

    private suspend fun ApplicationTestBuilder.create(work: MailboxCreationWork?): HttpResponse =
        client.post("/v1/mailboxes") {
            contentType(ContentType.Application.Json)
            setBody(
                serverJson.encodeToString(
                    CreateMailboxRequest(
                        nodeId = "node-a",
                        nodeEndpoint = "http://mailbox",
                        expiresAtEpochMilliseconds = System.currentTimeMillis() + 60_000L,
                        creationWork = work
                    )
                )
            )
        }

    private companion object {
        const val BITS = 8
    }
}

/** Solves the mailbox proof of work the way the app does, for tests. */
internal fun solvedWork(
    bits: Int,
    createdAt: Long = System.currentTimeMillis()
): MailboxCreationWork {
    val stamp = UUID.randomUUID().toString()
    var nonce = 0L
    while (leadingZeroBits(MailboxCreationWork(stamp, createdAt, nonce)) < bits) nonce++
    return MailboxCreationWork(stamp, createdAt, nonce)
}

private fun leadingZeroBits(work: MailboxCreationWork): Int {
    val digest =
        MessageDigest
            .getInstance("SHA-256")
            .digest("sparrow-mailbox-work-v1|${work.stamp}|${work.createdAtEpochMilliseconds}|${work.nonce}".encodeToByteArray())
    var bits = 0
    for (byte in digest) {
        val value = byte.toInt() and 0xFF
        if (value != 0) return bits + Integer.numberOfLeadingZeros(value) - 24
        bits += 8
    }
    return bits
}
