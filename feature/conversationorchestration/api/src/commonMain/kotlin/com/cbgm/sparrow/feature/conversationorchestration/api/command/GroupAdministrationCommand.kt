package com.cbgm.sparrow.feature.conversationorchestration.api.command

sealed interface GroupAdministrationCommand : ConversationCommand {
    data class CreateGroup(
        val title: String,
        val contactIds: Set<String>
    ) : GroupAdministrationCommand

    data class AddMembers(
        val groupId: String,
        val contactIds: Set<String>
    ) : GroupAdministrationCommand

    data class RemoveMember(
        val groupId: String,
        val contactId: String
    ) : GroupAdministrationCommand

    data class PromoteMember(
        val groupId: String,
        val contactId: String
    ) : GroupAdministrationCommand

    data class TransferAdminAndLeave(
        val groupId: String,
        val contactId: String
    ) : GroupAdministrationCommand

    data class LeaveGroup(
        val groupId: String
    ) : GroupAdministrationCommand

    data class DeleteGroup(
        val groupId: String
    ) : GroupAdministrationCommand

    data class ChangeTitle(
        val groupId: String,
        val title: String
    ) : GroupAdministrationCommand

    data class ChangeDescription(
        val groupId: String,
        val description: String
    ) : GroupAdministrationCommand

    data class ChangeAvatar(
        val groupId: String,
        val bytes: ByteArray
    ) : GroupAdministrationCommand {
        override fun equals(other: Any?): Boolean =
            other is ChangeAvatar && groupId == other.groupId && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = 31 * groupId.hashCode() + bytes.contentHashCode()
    }

    data class RemoveAvatar(
        val groupId: String
    ) : GroupAdministrationCommand

    data class VerifyMember(
        val groupId: String,
        val contactId: String
    ) : GroupAdministrationCommand

    data class SynchronizeVerification(
        val groupId: String
    ) : GroupAdministrationCommand
}
