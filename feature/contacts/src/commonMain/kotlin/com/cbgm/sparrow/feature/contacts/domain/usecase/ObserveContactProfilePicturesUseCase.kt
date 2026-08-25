package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ObserveContactProfilePicturesUseCase(
    private val provider: RemoteProfilePictureProvider
) {
    operator fun invoke(contactIds: Set<String>): Flow<Map<String, ByteArray?>> {
        val ids = contactIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return flowOf(emptyMap())

        return combine(
            ids.map { contactId ->
                provider
                    .observe(contactId)
                    .map { picture -> contactId to picture.bytes }
                    .onStart { emit(contactId to null) }
                    .catch { emit(contactId to null) }
            }
        ) { pictures -> pictures.toMap() }
    }
}
