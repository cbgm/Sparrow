package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.feature.attachments.data.model.UploadedBlobDto
import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob

internal fun UploadedBlobDto.toDomain(): UploadedBlob =
    UploadedBlob(reference = reference, deleteCapability = deleteCapability)

internal fun UploadedBlob.toDto(): UploadedBlobDto =
    UploadedBlobDto(reference = reference, deleteCapability = deleteCapability)
