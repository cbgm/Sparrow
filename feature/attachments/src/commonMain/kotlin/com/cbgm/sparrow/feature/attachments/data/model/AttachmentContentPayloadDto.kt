package com.cbgm.sparrow.feature.attachments.data.model

internal sealed interface AttachmentContentPayloadDto {
    data class LocalFile(
        val localFilePath: String
    ) : AttachmentContentPayloadDto

    class Payload(
        val bytes: ByteArray
    ) : AttachmentContentPayloadDto
}
