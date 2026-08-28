package com.cbgm.sparrow.feature.chats.domain.usecase.profile

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ObserveRemoteProfilePicturesUseCase(
    private val provider: RemoteProfilePictureProvider
) {
    operator fun invoke(contactId: String): Flow<ByteArray?> =
        provider
            .observe(contactId)
            .map { picture -> picture.bytes }
            .catch { emit(null) }
            .onStart { emit(null) }

    operator fun invoke(contactIds: Set<String>): Flow<Map<String, ByteArray?>> {
        val ids = contactIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return flowOf(emptyMap())

        val pictureFlows =
            ids.map { contactId ->
                provider
                    .observe(contactId)
                    .map { picture -> contactId to picture.bytes }
                    .catch { emit(contactId to null) }
                    .onStart { emit(contactId to null) }
            }

        return combine(pictureFlows) { pictures -> pictures.toMap() }
    }
}
