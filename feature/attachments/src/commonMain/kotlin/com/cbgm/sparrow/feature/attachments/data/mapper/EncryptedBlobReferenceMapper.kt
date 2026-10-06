package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.protocol.attachment.EncryptedBlobReference

internal fun EncryptedBlobReferenceDto.toProtocol(): EncryptedBlobReference =
    EncryptedBlobReference(
        nodeId = nodeId,
        blobId = blobId,
        readCapability = readCapability,
        ciphertextByteSize = ciphertextByteSize,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        encryptionKey = encryptionKey.copyOf(),
        nonce = nonce.copyOf(),
        ciphertextSha256 = ciphertextSha256.copyOf()
    )

internal fun EncryptedBlobReference.toDto(): EncryptedBlobReferenceDto =
    EncryptedBlobReferenceDto(
        nodeId = nodeId,
        blobId = blobId,
        readCapability = readCapability,
        ciphertextByteSize = ciphertextByteSize,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        encryptionKey = encryptionKey.copyOf(),
        nonce = nonce.copyOf(),
        ciphertextSha256 = ciphertextSha256.copyOf()
    )
