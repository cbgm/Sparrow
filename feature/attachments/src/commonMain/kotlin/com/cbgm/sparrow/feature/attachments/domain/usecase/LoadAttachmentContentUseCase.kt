package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

class LoadAttachmentContentUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend fun localFile(partId: String, groupId: String? = null): Result<String> =
        repository.loadLocalFile(partId, groupId)

    suspend fun location(partId: String, groupId: String? = null): Result<CurrentLocation> =
        repository.loadLocation(partId, groupId)

    suspend fun contact(partId: String, groupId: String? = null): Result<SharedContact> =
        repository.loadContact(partId, groupId)
}
