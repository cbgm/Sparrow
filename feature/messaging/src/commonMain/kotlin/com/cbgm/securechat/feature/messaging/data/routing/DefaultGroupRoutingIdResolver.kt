package com.cbgm.securechat.feature.messaging.data.routing

import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.feature.messaging.application.routing.GroupRoutingIdResolver
import com.cbgm.securechat.feature.transport.routing.RoutingIdGenerator

class DefaultGroupRoutingIdResolver(
    private val chatDao: ChatDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val routingIdGenerator: RoutingIdGenerator
) : GroupRoutingIdResolver {
    override suspend fun resolve(
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

    override suspend fun resolveMembers(groupId: String): Result<Map<String, String>> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            currentMemberKeys(groupId)
                .associate { memberKey -> memberKey.contactId to memberKey.toRoutingId() }
        }

    override fun resolveRemovedMember(signingPublicKey: ByteArray): Result<String> =
        runCatching {
            require(signingPublicKey.isNotEmpty()) { "Removed member signing public key must not be empty" }
            routingIdGenerator
                .deriveFromSigningPublicKey(signingPublicKey)
                .getOrThrow()
        }

    override suspend fun resolveForMessage(
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
            if (conversation.type != GROUP_CONVERSATION_TYPE) {
                return@runCatching null
            }

            resolve(
                groupId = conversation.id,
                contactId = contactId
            ).getOrThrow()
        }

    override suspend fun resolveContactId(routingId: String): Result<String?> =
        runCatching {
            require(routingId.isNotBlank()) { "Routing ID must not be blank" }

            groupSecurityDao
                .findAllCurrentMemberKeys()
                .firstOrNull { memberKey -> memberKey.toRoutingId() == routingId }
                ?.contactId
        }

    private suspend fun currentMemberKeys(groupId: String): List<GroupMemberKeyEntity> {
        val state =
            groupSecurityDao.findState(groupId)
                ?: error("Group security state was not found")
        return groupSecurityDao.findMemberKeys(
            groupId = groupId,
            epoch = state.currentEpoch
        )
    }

    private fun GroupMemberKeyEntity.toRoutingId(): String =
        routingIdGenerator
            .deriveFromSigningPublicKey(signingPublicKey)
            .getOrThrow()

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
