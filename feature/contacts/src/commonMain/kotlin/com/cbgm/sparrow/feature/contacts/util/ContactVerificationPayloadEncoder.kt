package com.cbgm.sparrow.feature.contacts.util

import com.cbgm.sparrow.core.crypto.util.ByteArrays

class ContactVerificationPayloadEncoder {
    fun encodeReceipt(
        packetId: String,
        version: Int,
        receiptId: String,
        verifiedAtEpochMilliseconds: Long,
        senderEncryptionPublicKey: ByteArray,
        senderSigningPublicKey: ByteArray,
        verifiedEncryptionPublicKey: ByteArray,
        verifiedSigningPublicKey: ByteArray
    ): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.withLengthPrefix("Sparrow.ContactVerificationReceipt".encodeToByteArray()),
            ByteArrays.withLengthPrefix(packetId.encodeToByteArray()),
            ByteArrays.withLengthPrefix(ByteArrays.encodeInt(version)),
            ByteArrays.withLengthPrefix(receiptId.encodeToByteArray()),
            ByteArrays.withLengthPrefix(ByteArrays.encodeLong(verifiedAtEpochMilliseconds)),
            ByteArrays.withLengthPrefix(senderEncryptionPublicKey),
            ByteArrays.withLengthPrefix(senderSigningPublicKey),
            ByteArrays.withLengthPrefix(verifiedEncryptionPublicKey),
            ByteArrays.withLengthPrefix(verifiedSigningPublicKey)
        )
}
