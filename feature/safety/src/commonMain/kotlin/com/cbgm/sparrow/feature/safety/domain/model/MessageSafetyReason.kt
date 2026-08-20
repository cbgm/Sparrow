package com.cbgm.sparrow.feature.safety.domain.model

enum class MessageSafetyReason {
    SUSPICIOUS_LINK,
    LOOKALIKE_DOMAIN,
    MIXED_SCRIPT_DOMAIN,
    IP_ADDRESS_LINK,
    URL_SHORTENER,
    URGENT_ACTION_REQUEST,
    CREDENTIAL_REQUEST,
    PAYMENT_REQUEST,
    PRIVATE_KEY_REQUEST
}
