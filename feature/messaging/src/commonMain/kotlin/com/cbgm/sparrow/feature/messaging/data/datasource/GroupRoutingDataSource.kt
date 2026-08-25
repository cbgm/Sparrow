package com.cbgm.sparrow.feature.messaging.data.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator

class GroupRoutingDataSource(
    private val chatDao: ChatDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val routingIdGenerator: RoutingIdGenerator
) {
    suspend fun resolve(
        groupId: String,
        contactId: String
    ): Result<String> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }

            currentMemberKeys(groupId)
                .firstOrNull { memberKey -> memberKey.contactId == contactId }
                ?.toRoutingId()
                ?: error("Contact is not a member of the current group epoch")
        }

    suspend fun resolveMembers(groupId: String): Result<Map<String, String>> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            currentMemberKeys(groupId)
                .associate { memberKey -> memberKey.contactId to memberKey.toRoutingId() }
        }

    fun resolveRemovedMember(signingPublicKey: ByteArray): Result<String> =
        runCatching {
            require(signingPublicKey.isNotEmpty()) {
                "Removed member signing public key must not be empty"
            }
            routingIdGenerator
                .deriveFromSigningPublicKey(signingPublicKey)
                .getOrThrow()
        }

    suspend fun resolveForMessage(
        messageId: String,
        contactId: String
    ): Result<String?> =
        runCatching {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }

            val message = chatDao.findMessageById(messageId) ?: return@runCatching null
            val conversation =
                chatDao.findConversationById(message.conversationId)
                    ?: return@runCatching null
            if (conversation.type != GROUP_CONVERSATION_TYPE) return@runCatching null

            resolve(conversation.id, contactId).getOrThrow()
        }

    suspend fun resolveContactId(routingId: String): Result<String?> =
        runCatching {
            require(routingId.isNotBlank()) { "Routing ID must not be blank" }
            groupSecurityDao
                .findAllCurrentMemberKeys()
                .firstOrNull { memberKey -> memberKey.toRoutingId() == routingId }
                ?.contactId
        }

    private suspend fun currentMemberKeys(groupId: String): List<GroupMemberKeyEntity> {
        val state = groupSecurityDao.findState(groupId) ?: error("Group security state was not found")
        return groupSecurityDao.findMemberKeys(groupId, state.currentEpoch)
    }

    private fun GroupMemberKeyEntity.toRoutingId(): String =
        routingIdGenerator
            .deriveFromSigningPublicKey(signingPublicKey)
            .getOrThrow()

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
