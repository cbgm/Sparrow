package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.feature.membership.crypto.GroupKeyConfirmation
import com.cbgm.sparrow.feature.membership.crypto.GroupKeyStore
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox

/** Only Membership loads the installed epoch key and signs the membership-ready packet. */
internal class GroupReadyAcknowledgementDataSource(
    private val groupKeyStore: GroupKeyStore,
    private val cryptoHash: CryptoHash,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val protocolOutbox: ProtocolOutbox
) {
    suspend fun send(
        groupId: String,
        epoch: Int,
        welcomePacketId: String,
        recipientContactId: String
    ) {
        val groupKey = groupKeyStore.load(groupId, epoch)
            ?: error("Installed group key was not found for ready acknowledgement")
        val acknowledgement = membershipPacketProtocol.createReadyAcknowledgement(
            groupId = groupId,
            epoch = epoch,
            welcomePacketId = welcomePacketId,
            keyConfirmation = GroupKeyConfirmation.create(cryptoHash, groupId, epoch, groupKey),
            memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        ).getOrThrow()
        protocolOutbox.enqueue(recipientContactId, acknowledgement).getOrThrow()
    }
}
