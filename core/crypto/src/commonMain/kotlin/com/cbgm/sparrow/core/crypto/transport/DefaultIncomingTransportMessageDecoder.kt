package com.cbgm.sparrow.core.crypto.transport

class DefaultIncomingTransportMessageDecoder(
    private val payloadCodec: TransportPayloadCodec,
    private val transportCipher: TransportMessageCipher
) : IncomingTransportMessageDecoder {
    override suspend fun decode(
        encodedPayload: String,
        localPublicKey: ByteArray,
        localPrivateKey: ByteArray
    ): DecodedTransportMessage {
        val payload =
            payloadCodec
                .decode(
                    encoded = encodedPayload
                ).getOrElse { error ->
                    return DecodedTransportMessage
                        .InvalidPacket(
                            cause = error
                        )
                }

        return when (payload.mode) {
            TransportEncryptionMode.PLAINTEXT -> {
                decodePlaintext(payload = payload)
            }

            TransportEncryptionMode.SEALED_BOX -> {
                decodeSealedBox(
                    payload = payload,
                    localPublicKey = localPublicKey,
                    localPrivateKey = localPrivateKey
                )
            }
        }
    }

    private fun decodePlaintext(payload: EncryptedTransportPayload): DecodedTransportMessage =
        try {
            /*
             * This validates that the packet contains valid UTF-8.
             *
             * No cryptographic decryption is performed for a
             * PLAINTEXT packet.
             */
            val text = payload.payload.decodeToString(throwOnInvalidSequence = true)

            DecodedTransportMessage.Readable(
                plaintext = text.encodeToByteArray(),
                mode = TransportEncryptionMode.PLAINTEXT
            )
        } catch (
            error: Throwable
        ) {
            DecodedTransportMessage.InvalidPlaintext(cause = error)
        }

    private suspend fun decodeSealedBox(
        payload: EncryptedTransportPayload,
        localPublicKey: ByteArray,
        localPrivateKey: ByteArray
    ): DecodedTransportMessage =
        transportCipher
            .decryptFromSender(
                encryptedPayload = payload,
                localPublicKey = localPublicKey,
                localPrivateKey = localPrivateKey
            ).fold(
                onSuccess = { plaintext ->
                    /*
                     * The decrypted bytes still need to be valid UTF-8
                     * because chat messages are textual.
                     */
                    try {
                        val text = plaintext.decodeToString(throwOnInvalidSequence = true)

                        DecodedTransportMessage.Readable(
                            plaintext = text.encodeToByteArray(),
                            mode = TransportEncryptionMode.SEALED_BOX
                        )
                    } catch (
                        error: Throwable
                    ) {
                        DecodedTransportMessage.DecryptionFailed(cause = error)
                    }
                },
                onFailure = { error ->
                    /*
                     * Never try to decode failed ciphertext directly.
                     */
                    DecodedTransportMessage.DecryptionFailed(cause = error)
                }
            )
}
