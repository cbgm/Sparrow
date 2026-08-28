package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveContactProfilePictureUseCase(
    private val provider: RemoteProfilePictureProvider
) {
    operator fun invoke(contactId: String): Flow<ByteArray?> =
        provider.observe(contactId).map { picture -> picture.bytes }
}
