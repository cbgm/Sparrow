package com.cbgm.securechat.feature.messaging.application.relay

interface GroupRelayIdResolver {
    suspend fun resolve(
        groupId: String,
        contactId: String
    ): Result<String>

    suspend fun resolveMembers(groupId: String): Result<Map<String, String>>

    fun resolveRemovedMember(signingPublicKey: ByteArray): Result<String>

    suspend fun resolveForMessage(
        messageId: String,
        contactId: String
    ): Result<String?>

    suspend fun resolveContactId(relayId: String): Result<String?>
}
