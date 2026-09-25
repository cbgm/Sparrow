package com.cbgm.sparrow.feature.conversationorchestration.runtime.routing

import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.membership.domain.model.GroupTransportRoutingMember
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupTransportRoutingMembersUseCase
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator

class GroupRoutingResolver(
    private val conversationPort: ConversationPort,
    private val getGroupRoutingMembers: GetGroupTransportRoutingMembersUseCase,
    private val routingIdGenerator: RoutingIdGenerator
) {
    suspend fun resolve(
        groupId: String,
        contactId: String
    ): String {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }

        return currentMemberKeys(groupId)
            .firstOrNull { memberKey -> memberKey.contactId == contactId }
            ?.toRoutingId()
            ?: error("Contact is not a member of the current group epoch")
    }

    @Suppress("unused") // Retained recipient-routing API; current callers resolve recipients individually.
    suspend fun resolveMembers(groupId: String): Map<String, String> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return currentMemberKeys(groupId)
            .associate { memberKey -> memberKey.contactId to memberKey.toRoutingId() }
    }

    suspend fun resolveIndicatorMembers(groupId: String): Map<String, String> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return getGroupRoutingMembers(groupId).getOrThrow().orEmpty()
            .associate { memberKey -> memberKey.contactId to memberKey.toRoutingId() }
    }

    fun resolveRemovedMember(signingPublicKey: ByteArray): String {
        require(signingPublicKey.isNotEmpty()) {
            "Removed member signing public key must not be empty"
        }
        return routingIdGenerator
            .deriveFromSigningPublicKey(signingPublicKey)
            .getOrThrow()
    }

    suspend fun resolveForMessage(
        messageId: String,
        contactId: String
    ): String? {
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }

        val groupId = conversationPort.findGroupIdForMessage(messageId).getOrThrow() ?: return null
        return resolve(groupId, contactId)
    }

    suspend fun resolveContactId(routingId: String): String? {
        require(routingId.isNotBlank()) { "Routing ID must not be blank" }
        return getGroupRoutingMembers
            .allCurrent().getOrThrow()
            .firstOrNull { memberKey -> memberKey.toRoutingId() == routingId }
            ?.contactId
    }

    private suspend fun currentMemberKeys(groupId: String): List<GroupTransportRoutingMember> =
        getGroupRoutingMembers(groupId).getOrThrow()
            ?: error("Group security state was not found")

    private fun GroupTransportRoutingMember.toRoutingId(): String =
        routingIdGenerator
            .deriveFromSigningPublicKey(signingPublicKey)
            .getOrThrow()
}
