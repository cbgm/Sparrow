package com.cbgm.sparrow.core.crypto.group

interface GroupCrypto {
    suspend fun generateGroupKey(): Result<ByteArray>

    suspend fun generateInvitationChallenge(): Result<ByteArray>

    suspend fun wrapGroupKey(
        groupKey: ByteArray,
        recipientEncryptionPublicKey: ByteArray
    ): Result<ByteArray>

    suspend fun unwrapGroupKey(
        wrappedGroupKey: ByteArray,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray
    ): Result<ByteArray>

    suspend fun encryptMessage(
        plaintext: ByteArray,
        associatedData: ByteArray,
        groupKey: ByteArray
    ): Result<GroupCiphertext>

    suspend fun decryptMessage(
        ciphertext: GroupCiphertext,
        associatedData: ByteArray,
        groupKey: ByteArray
    ): Result<ByteArray>

    suspend fun sign(
        payload: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<ByteArray>

    suspend fun verify(
        payload: ByteArray,
        signature: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit>
}
