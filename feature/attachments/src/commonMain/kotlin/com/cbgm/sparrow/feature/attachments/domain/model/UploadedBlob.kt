package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference

data class UploadedBlob(
    val reference: EncryptedBlobReference,
    val deleteCapability: String
)
