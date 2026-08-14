package com.cbgm.sparrow.feature.chats.domain.model.direct

enum class ContactSecurityState {
    /** Phone-book contact only. We do not possess this contact's public keys. */
    NO_REMOTE_PUBLIC_KEYS,

    /** We possess their public keys, but they do not yet possess ours. */
    ONE_WAY_KEYS,

    /** Both parties possess each other's public keys, but neither verification is known. */
    MUTUAL_KEYS_UNVERIFIED,

    /** This device verified the contact, but the contact has not verified this device yet. */
    MUTUAL_KEYS_VERIFIED_BY_ME,

    /** The contact verified this device, but this device has not verified the contact yet. */
    MUTUAL_KEYS_VERIFIED_BY_CONTACT,

    /** Both devices independently verified the exact current identity keys. */
    MUTUAL_KEYS_VERIFIED
}
