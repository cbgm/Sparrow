package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference

internal data class UploadedBlobDto(
    val reference: EncryptedBlobReference,
    val deleteCapability: String
)
