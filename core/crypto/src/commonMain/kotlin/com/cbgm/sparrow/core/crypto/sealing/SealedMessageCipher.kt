package com.cbgm.sparrow.core.crypto.sealing

/**
 * Seals messages for one X25519 key, the way the server's `X25519KeyPair` opens them: alias claims for a presence
 * directory and onion layers for relays. Replies come back sealed with a fresh key the app put into the request.
 */
interface SealedMessageCipher {
    /** [context] separates the uses, so a message sealed for one cannot be opened as another. */
    suspend fun seal(
        message: ByteArray,
        recipientPublicKey: ByteArray,
        context: String
    ): Result<ByteArray>

    /** Null when [sealed] was not sealed with [replyKey] or changed on the way. */
    suspend fun openReply(
        sealed: ByteArray,
        replyKey: ByteArray
    ): ByteArray?

    suspend fun newReplyKey(): ByteArray
}
