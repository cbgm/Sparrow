package com.cbgm.sparrow.feature.chats.domain.model

enum class MessageContentStatus {
    /**
     * Content is available and can be displayed.
     */
    READABLE,

    /**
     * The transport packet itself could not be decoded.
     */
    INVALID_PACKET,

    /**
     * A plaintext packet did not contain valid UTF-8.
     */
    INVALID_PLAINTEXT_PACKET,

    /**
     * A sealed-box packet could not be decrypted.
     */
    TRANSPORT_DECRYPTION_FAILED
}
