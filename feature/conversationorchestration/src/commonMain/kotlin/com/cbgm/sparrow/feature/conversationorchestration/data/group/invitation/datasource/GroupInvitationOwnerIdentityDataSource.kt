package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation.datasource

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource

internal class GroupInvitationOwnerIdentityDataSource(
    private val contactDao: ContactDao,
    private val contactKeyExchangeDataSource: ContactKeyExchangeDataSource
) {
    suspend fun stage(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Boolean {
        val existing = contactDao.findPublicIdentityByContactId(contactId) ?: return false
        val sameIdentity =
            existing.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
                existing.signingPublicKey.contentEquals(signingPublicKey)
        if (sameIdentity) return false

        return contactKeyExchangeDataSource.storeRemoteHandshakeIdentity(
            contactId = contactId,
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey
        )
    }
}
