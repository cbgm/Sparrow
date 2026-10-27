package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckMessageEditableUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupPayloadUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDeletableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindEditableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordPacketSendFailureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireGroupConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireMessageWithoutPaymentUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedAttachments
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedDirectMessages
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedGroupMembership
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedGroupMessages
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedGroupSecurity
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedOutbox
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedPlaintextCodec
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedProfilePictures
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedSigningKeys
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupMessageMembershipAccessUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Editing or deleting a group message members already have takes no lock, so it never waits for a send or flush
 * that uploads attachments. Only a message nobody has yet is changed under the group send lock, where the
 * recipients are checked again.
 */
class GroupEditLockTest {
    private val engine = WorkflowEngine(WorkflowLocks(), WorkflowRegistry { emptyList() })
    private val recipients = FindGroupRecipientsUseCase(UnusedGroupMessages)
    private val encrypt =
        EncryptGroupOperationUseCase(
            UnusedPlaintextCodec,
            EncryptGroupPayloadUseCase(UnusedGroupSecurity, UnusedSigningKeys, UnusedProfilePictures)
        )
    private val enqueue = EnqueueGroupPacketsUseCase(UnusedOutbox, RecordPacketSendFailureUseCase(engine))
    private val access =
        CheckGroupMessageAccessUseCase(
            UnusedGroupMessages,
            RequireGroupConversationUseCase(UnusedGroupMessages),
            GetGroupMessageMembershipAccessUseCase(UnusedGroupMembership)
        )
    private val findEditable =
        FindEditableGroupMessageUseCase(
            UnusedGroupMessages,
            FindGroupMessageUseCase(UnusedGroupMessages),
            CheckMessageEditableUseCase(UnusedDirectMessages, UnusedGroupMessages, UnusedAttachments)
        )
    private val findDeletable =
        FindDeletableGroupMessageUseCase(
            FindGroupMessageUseCase(UnusedGroupMessages),
            RequireMessageWithoutPaymentUseCase(UnusedAttachments)
        )
    private val discard = DiscardGroupMessagesUseCase(UnusedGroupMessages, UnusedAttachments)

    private val edit =
        EditGroupMessageWorkflow(engine, UnusedGroupMessages, access, findEditable, recipients, encrypt, enqueue)
    private val editUnsent =
        EditUnsentGroupMessageWorkflow(UnusedGroupMessages, findEditable, recipients, encrypt, enqueue)
    private val delete =
        DeleteGroupMessageWorkflow(engine, access, findDeletable, recipients, encrypt, enqueue, discard)
    private val deleteUnsent = DeleteUnsentGroupMessageWorkflow(findDeletable, recipients, encrypt, enqueue, discard)

    private val editCommand = GroupMessageCommand.EditMessage(GROUP_ID, MESSAGE_ID, "new text")
    private val deleteCommand = GroupMessageCommand.DeleteMessage(GROUP_ID, MESSAGE_ID)
    private val unsentEditCommand = MessageCommand.EditUnsentGroupMessage(GROUP_ID, MESSAGE_ID, "new text")
    private val unsentDeleteCommand = MessageCommand.DeleteUnsentGroupMessage(GROUP_ID, MESSAGE_ID)

    @Test
    fun onlyChangesOfAMessageNobodyHasTakeTheGroupSendLock() {
        assertNull(edit.lockKey(editCommand))
        assertNull(delete.lockKey(deleteCommand))
        assertEquals(groupSendLockKey(GROUP_ID), editUnsent.lockKey(unsentEditCommand))
        assertEquals(groupSendLockKey(GROUP_ID), deleteUnsent.lockKey(unsentDeleteCommand))
    }

    @Test
    fun editOfAMessageMembersHaveIsSentWithoutTheSendLock() {
        val transition =
            edit.transition(
                EditGroupMessageWorkflow.Step.FindRecipients(editCommand),
                EditGroupMessageWorkflow.Outcome.RecipientsFound(listOf("alice"))
            )

        assertEquals(
            Transition.Next(EditGroupMessageWorkflow.Step.EncryptEdit(listOf("alice"), editCommand)),
            transition
        )
    }

    @Test
    fun editOfAMessageNobodyHasRunsUnderTheSendLock() {
        val transition =
            edit.transition(
                EditGroupMessageWorkflow.Step.FindRecipients(editCommand),
                EditGroupMessageWorkflow.Outcome.RecipientsFound(emptyList())
            )

        assertEquals(Transition.Next(EditGroupMessageWorkflow.Step.EditUnsentMessage(editCommand)), transition)
    }

    @Test
    fun unsentEditOfAMessageAFlushSentMeanwhileIsSentToo() {
        val transition =
            editUnsent.transition(
                EditUnsentGroupMessageWorkflow.Step.FindRecipients(unsentEditCommand),
                EditUnsentGroupMessageWorkflow.Outcome.RecipientsFound(listOf("alice"))
            )

        assertEquals(
            Transition.Next(EditUnsentGroupMessageWorkflow.Step.EncryptEdit(listOf("alice"), unsentEditCommand)),
            transition
        )
    }

    @Test
    fun deleteOfAMessageMembersHaveIsSentWithoutTheSendLockAndAnUnsentOneUnderIt() {
        val message = ownMessage()
        val step = DeleteGroupMessageWorkflow.Step.FindRecipients(message, deleteCommand)

        assertEquals(
            Transition.Next(DeleteGroupMessageWorkflow.Step.EncryptDelete(message, listOf("alice"), deleteCommand)),
            delete.transition(step, DeleteGroupMessageWorkflow.Outcome.RecipientsFound(message, listOf("alice")))
        )
        assertEquals(
            Transition.Next(DeleteGroupMessageWorkflow.Step.DeleteUnsentMessage(message, deleteCommand)),
            delete.transition(step, DeleteGroupMessageWorkflow.Outcome.RecipientsFound(message, emptyList()))
        )
    }

    private fun ownMessage() =
        StoredMessage(
            id = MESSAGE_ID,
            conversationId = GROUP_ID,
            packetId = null,
            replyToMessageId = null,
            transportPayload = null,
            transportMode = MessageTransportMode.GROUP_END_TO_END_ENCRYPTED,
            contentStatus = MessageContentStatus.READABLE,
            deliveryStatus = MessageDeliveryStatus.SENT,
            senderContactId = null,
            readReceiptSent = false,
            isMine = true,
            createdAtEpochMilliseconds = 1L
        )

    private companion object {
        const val GROUP_ID = "group-1"
        const val MESSAGE_ID = "message-1"
    }
}
