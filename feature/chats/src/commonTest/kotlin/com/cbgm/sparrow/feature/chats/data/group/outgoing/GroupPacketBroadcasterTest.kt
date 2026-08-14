package com.cbgm.sparrow.feature.chats.data.group.outgoing

import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupPacketBroadcasterTest {
    @Test
    fun oneFailedRecipientDoesNotStopLaterRecipientsFromBeingQueued() =
        runTest {
            val outbox = RecordingProtocolOutbox(failingContactId = "contact-2")
            val broadcaster = GroupPacketBroadcaster(outbox)

            val result =
                broadcaster.enqueueAll(
                    linkedMapOf(
                        "contact-1" to invitePacket("packet-1"),
                        "contact-2" to invitePacket("packet-2"),
                        "contact-3" to invitePacket("packet-3")
                    )
                )

            assertTrue(result.isFailure)
            assertEquals(
                listOf("contact-1", "contact-2", "contact-3"),
                outbox.attemptedContactIds
            )
        }

    private fun invitePacket(packetId: String): GroupInvitePacket =
        GroupInvitePacket(
            packetId = packetId,
            invitationId = "invite-$packetId",
            groupId = "group-1",
            title = "Group",
            createdAtEpochMilliseconds = 1L,
            expiresAtEpochMilliseconds = 2L,
            challenge = byteArrayOf(1),
            ownerEncryptionPublicKey = byteArrayOf(2),
            ownerSigningPublicKey = byteArrayOf(3),
            ownerSignature = byteArrayOf(4)
        )

    private class RecordingProtocolOutbox(
        private val failingContactId: String
    ) : ProtocolOutbox {
        val attemptedContactIds = mutableListOf<String>()

        override suspend fun enqueue(
            contactId: String,
            packet: SparrowPacket
        ): Result<ProtocolOutboxItem> {
            attemptedContactIds += contactId
            return if (contactId == failingContactId) {
                Result.failure(IllegalStateException("queue failed"))
            } else {
                Result.success(outboxItem(contactId, packet.packetId))
            }
        }

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> =
            Result.success(emptyList())

        override suspend fun markProcessing(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun markSent(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun markFailed(
            itemId: String,
            errorMessage: String
        ): Result<Unit> = Result.success(Unit)

        override suspend fun retry(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun resend(packetId: String): Result<Unit> = Result.success(Unit)

        override suspend fun requeueInterrupted(): Result<Unit> = Result.success(Unit)

        override suspend fun retryFailed(): Result<Unit> = Result.success(Unit)

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> =
            Result.success(null)

        private fun outboxItem(
            contactId: String,
            packetId: String
        ): ProtocolOutboxItem =
            ProtocolOutboxItem(
                id = "item-$packetId",
                contactId = contactId,
                packetId = packetId,
                encodedPacket = byteArrayOf(1),
                status = com.cbgm.sparrow.core.protocol.outbox.OutboxStatus.PENDING,
                attemptCount = 0,
                lastError = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
    }
}
