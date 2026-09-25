package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.SparrowIdentity
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity

/** Compatibility projection for callers migrating from Contact.sparrowIdentity.
 * Identity remains the sole owner of keys, exchange state and verification persistence.
 */
internal fun Contact.withRemoteIdentity(identity: RemotePeerIdentity?): Contact =
    copy(
        sparrowIdentity = identity?.let { remote ->
            SparrowIdentity(
                encryptionPublicKey = remote.encryptionPublicKey.copyOf(),
                signingPublicKey = remote.signingPublicKey.copyOf(),
                verificationStatus = remote.verificationStatus,
                verifiedByContact = remote.verifiedByContact,
                locallyImported = remote.locallyImported,
                keyExchangeStatus = remote.keyExchangeStatus,
                updatedAtEpochMilliseconds = remote.updatedAtEpochMilliseconds
            )
        }
    )
