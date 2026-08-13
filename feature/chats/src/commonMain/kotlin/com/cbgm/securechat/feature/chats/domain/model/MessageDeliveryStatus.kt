package com.cbgm.securechat.feature.chats.domain.model

enum class MessageDeliveryStatus {
    /**
     * Incoming messages do not use an outgoing delivery state.
     */
    NOT_APPLICABLE,

    /**
     * Packet is persisted in the outbox.
     */
    QUEUED,

    /**
     * The outbox processor is currently attempting transport.
     */
    SENDING,

    /**
     * The gateway accepted the outgoing envelope.
     *
     * This does not yet prove recipient storage.
     */
    SENT,

    /**
     * The recipient successfully decoded and stored the message.
     */
    DELIVERED,

    /**
     * Reserved for the later read-receipt implementation.
     */
    READ,

    /**
     * The latest outgoing transport attempt failed.
     */
    FAILED
}
