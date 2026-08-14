package com.cbgm.sparrow.feature.identity.domain.model

/**
 * Describes the current state of the user's local cryptographic identity.
 */
enum class IdentityStatus {
    /**
     * Neither public keys nor private keys exist.
     *
     * The user can safely create a new identity.
     */
    NOT_CREATED,

    /**
     * Both the public identity and protected private keys exist.
     *
     * The identity is ready for use.
     */
    READY,

    /**
     * Only part of the identity exists.
     *
     * Examples:
     *
     * - public keys exist but private keys are missing
     * - private keys exist but public keys are missing
     *
     * We must not silently create a new identity in this state,
     * because doing so could permanently break access to encrypted
     * conversation history.
     */
    INCOMPLETE
}
