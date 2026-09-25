package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.crypto.group.GroupKeyConfirmation
import com.cbgm.sparrow.core.crypto.group.GroupKeyStore
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.packet.GroupProtocolPayloadEncoder
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.membership.data.model.CreatedGroupSecurityDto
import com.cbgm.sparrow.feature.membership.data.model.GroupWelcomeRecipientDto
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole

/** Group epoch changes are Membership-owned. No Chats-implemented datasource or callback. */
internal class GroupEpochSecurityDataSource(
    private val groupCrypto: GroupCrypto,
    private val cryptoHash: CryptoHash,
    private val payloadEncoder: GroupProtocolPayloadEncoder,
    private val securityStore: GroupSecurityStoreDataSource,
    private val groupKeyDataSource: GroupKeyStore
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
            securityStore.replaceCurrentEpoch(
                state = nextState,
                memberKeys = memberKeys
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

            groupKeyDataSource
                .deleteBefore(
                    groupId = groupId,
                    epoch = nextEpoch
                )

            CreatedGroupSecurityDto(welcomePacketsByContactId = packets)
        }

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
