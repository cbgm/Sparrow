package com.cbgm.sparrow.feature.conversationorchestration.data.group.membership

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipPeerDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto

internal class GroupMembershipPeerDataSourceImpl(
    private val contactDao: ContactDao
) : GroupMembershipPeerDataSource {
    override suspend fun findPeer(contactId: String): GroupMembershipPeerDto? {
        val stored = contactDao.findById(contactId) ?: return null
        val preferredPhoneNumber =
            stored.phoneNumbers.firstOrNull { phoneNumber ->
                phoneNumber.id == stored.contact.preferredPhoneNumberId
            } ?: stored.phoneNumbers.firstOrNull()
        val identity = stored.publicIdentity
        return GroupMembershipPeerDto(
            id = stored.contact.id,
            displayName = stored.contact.displayName,
            preferredPhoneNumber = preferredPhoneNumber?.value,
            phoneNumbers = stored.phoneNumbers.map { phoneNumber -> phoneNumber.value },
            encryptionPublicKey = identity?.encryptionPublicKey?.copyOf(),
            signingPublicKey = identity?.signingPublicKey?.copyOf(),
            hasMutualIdentity =
                identity != null &&
                    identity.keyExchangeStatus == KEY_EXCHANGE_STATUS_MUTUAL &&
                    identity.encryptionPublicKey.isNotEmpty() &&
                    identity.signingPublicKey.isNotEmpty()
        )
    }

    private companion object {
        const val KEY_EXCHANGE_STATUS_MUTUAL = "MUTUAL"
    }
}
