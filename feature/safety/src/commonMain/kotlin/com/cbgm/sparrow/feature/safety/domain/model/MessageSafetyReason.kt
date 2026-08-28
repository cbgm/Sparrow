package com.cbgm.sparrow.feature.safety.domain.model

enum class MessageSafetyReason(
    val id: String
) {
    SUSPICIOUS_LINK("suspicious_link"),
    LOOKALIKE_DOMAIN("lookalike_domain"),
    MIXED_SCRIPT_DOMAIN("mixed_script_domain"),
    IP_ADDRESS_LINK("ip_address_link"),
    URL_SHORTENER("url_shortener"),
    URGENT_ACTION_REQUEST("urgent_action_request"),
    CREDENTIAL_REQUEST("credential_request"),
    PAYMENT_REQUEST("payment_request"),
    PRIVATE_KEY_REQUEST("private_key_request");

    companion object {
        fun fromId(id: String): MessageSafetyReason? = entries.firstOrNull { it.id == id }
    }
}
