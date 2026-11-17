package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** This device's invitation mailboxes: the current one first, then those a renewal replaced, until they expired. */
interface InvitationMailboxStore {
    suspend fun all(): List<LocalMailboxCredential>

    suspend fun replace(mailboxes: List<LocalMailboxCredential>)
}

class DataStoreInvitationMailboxStore(
    private val dataStore: SparrowDataStore,
    private val json: Json
) : InvitationMailboxStore {
    override suspend fun all(): List<LocalMailboxCredential> =
        dataStore
            .getString(KEY)
            ?.let { encoded -> runCatching { json.decodeFromString<List<StoredInvitationMailbox>>(encoded) }.getOrNull() }
            .orEmpty()
            .map(StoredInvitationMailbox::credential)

    override suspend fun replace(mailboxes: List<LocalMailboxCredential>) {
        val encoded = json.encodeToString(mailboxes.map(::StoredInvitationMailbox))
        dataStore.edit { putString(KEY, encoded) }
    }

    @Serializable
    private class StoredInvitationMailbox(
        val deliveryRoute: MailboxDeliveryRoute,
        val accessEndpoint: String,
        val retrievalCapability: String
    ) {
        constructor(credential: LocalMailboxCredential) : this(
            credential.deliveryRoute,
            credential.accessEndpoint,
            credential.retrievalCapability
        )

        fun credential(): LocalMailboxCredential =
            LocalMailboxCredential(
                contactId = InvitationMailboxRenewal.CONTACT_ID,
                deliveryRoute = deliveryRoute,
                accessEndpoint = accessEndpoint,
                retrievalCapability = retrievalCapability
            )
    }

    private companion object {
        const val KEY = "transport.invitation_mailboxes"
    }
}
