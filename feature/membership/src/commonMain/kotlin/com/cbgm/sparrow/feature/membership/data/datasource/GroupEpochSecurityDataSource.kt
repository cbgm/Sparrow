package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.transaction.DatabaseTransactionRunner
import com.cbgm.sparrow.feature.membership.crypto.GroupCrypto
import com.cbgm.sparrow.feature.membership.crypto.GroupKeyConfirmation
import com.cbgm.sparrow.feature.membership.crypto.GroupKeyStore
import com.cbgm.sparrow.feature.membership.data.model.CreatedGroupSecurityDto
import com.cbgm.sparrow.feature.membership.data.model.GroupWelcomeRecipientDto
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.protocol.packet.GroupProtocolPayloadEncoder

/**
 * Group epoch changes are Membership-owned. No Chats-implemented datasource or callback.
 *
 * A rotation commits the new epoch and queues its welcomes in one database transaction: a crash or a failed
 * enqueue in between would otherwise leave members without the key, and the next rotation would skip an epoch
 * for them. The previous epoch's key stays, so messages sent just before the change can still be read.
 */
internal class GroupEpochSecurityDataSource(
    private val groupCrypto: GroupCrypto,
    private val cryptoHash: CryptoHash,
    private val payloadEncoder: GroupProtocolPayloadEncoder,
    private val securityStore: GroupSecurityStoreDataSource,
    private val groupKeyDataSource: GroupKeyStore,
    private val packetBroadcaster: GroupPacketBroadcaster,
    private val transactions: DatabaseTransactionRunner
) {
    suspend fun verifyKeyConfirmation(groupId: String, epoch: Int, confirmation: ByteArray) {
        val key = groupKeyDataSource.load(groupId, epoch)
            ?: error("Group key was not found")
        check(GroupKeyConfirmation.create(cryptoHash, groupId, epoch, key).contentEquals(confirmation)) {
            "Group key confirmation does not match"
        }
    }

    suspend fun rotateOwnedGroup(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long,
        memberPayloads: List<GroupMemberPayload>,
        memberKeys: List<GroupMemberKeyEntity>,
        recipients: List<GroupWelcomeRecipientDto>,
        localSigningKeyPair: LocalSigningKeyPair,
        membershipChange: GroupMembershipChangePayload? = null
    ): Result<CreatedGroupSecurityDto> =
        safeSuspendCall {
            val existingState =
                securityStore.findState(groupId)
                    ?: error("Group security state was not found")
            check(existingState.localRole.isGroupAdminRole()) {
                "Only a group admin may rotate the group epoch"
            }
            check(existingState.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)) {
                "Local admin signing key does not match the current security state"
            }

            val nextEpoch = existingState.currentEpoch + 1
            check(memberKeys.all { memberKey -> memberKey.epoch == nextEpoch }) {
                "Every member key must belong to the next group epoch"
            }
            val groupKey = groupCrypto.generateGroupKey().getOrThrow()
            groupKeyDataSource
                .save(
                    groupId = groupId,
                    epoch = nextEpoch,
                    groupKey = groupKey
                )

            val nextState =
                existingState.copy(
                    currentEpoch = nextEpoch,
                    welcomePacketId = null,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )

            val packets =
                recipients.associate { recipient ->
                    val wrappedGroupKey =
                        groupCrypto
                            .wrapGroupKey(
                                groupKey = groupKey,
                                recipientEncryptionPublicKey = recipient.encryptionPublicKey
                            ).getOrThrow()
                    val unsignedPacket =
                        GroupCreatedPacket(
                            packetId = "group-welcome-$groupId-${recipient.invitationId}-$nextEpoch",
                            groupId = groupId,
                            title = title,
                            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                            epoch = nextEpoch,
                            members = memberPayloads,
                            wrappedGroupKey = wrappedGroupKey,
                            ownerSignature = UNSIGNED_PACKET_MARKER,
                            membershipChange = membershipChange
                        )
                    val signature =
                        groupCrypto
                            .sign(
                                payload = payloadEncoder.encodeWelcome(unsignedPacket),
                                signingPrivateKey = localSigningKeyPair.privateKey
                            ).getOrThrow()

                    recipient.contactId to unsignedPacket.copy(ownerSignature = signature)
                }

            transactions.inTransaction {
                securityStore.replaceCurrentEpoch(
                    state = nextState,
                    memberKeys = memberKeys
                )
                packetBroadcaster.enqueueAll(packets).getOrThrow()
            }

            groupKeyDataSource
                .deleteBefore(
                    groupId = groupId,
                    epoch = nextEpoch - 1
                )

            CreatedGroupSecurityDto(welcomePacketsByContactId = packets)
        }

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
