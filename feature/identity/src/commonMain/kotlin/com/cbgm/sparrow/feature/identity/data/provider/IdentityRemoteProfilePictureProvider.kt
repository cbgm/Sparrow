package com.cbgm.sparrow.feature.identity.data.provider

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureSnapshot
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IdentityRemoteProfilePictureProvider(
    private val repository: RemoteProfilePictureRepository
) : RemoteProfilePictureProvider {
    override fun observe(contactId: String): Flow<RemoteProfilePictureSnapshot> =
        repository.observe(contactId).map { picture ->
            RemoteProfilePictureSnapshot(
                contactId = picture.contactId,
                changedAtEpochMilliseconds = picture.changedAtEpochMilliseconds,
                bytes = picture.bytes
            )
        }
}
