package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.routing

import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.command.DeliveryCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DeliveryEventTarget
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.dispatch.ControlPacketDispatcher
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.dispatch.GroupPacketDispatcher
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.dispatch.groupIdOrNull
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.IncomingPacketDeferredException
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DeferredPacketWait
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DirectPeerAccess
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ReceivedPacket
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.IncomingDirectTransportPolicy
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.CheckGroupPacketAcceptedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.CheckGroupPacketReadyUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.ResolveControlConversationIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.ResolveDirectPeerAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.protocol.packet.OperationMessagePacket
import com.cbgm.sparrow.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket
import kotlin.reflect.KClass

/**
 * direct message:  CheckDirectAuthorization -> EnsureDirectConversation -> ReceiveDirectMessage | ApplyDirectOperation
 * group packet:    CheckGroupAccepted -> CheckGroupReady -> DispatchGroupPacket
 * receipt:         ApplyReceipt
 * anything else:   FindControlConversation -> DispatchControlPacket
 *
 * Direct and group packets never share handlers. Messages of a contact whose invitation was not accepted
 * are dropped without a chat being created. Packets that arrived too early — a message of a contact whose
 * invitation is still in flight, a group packet before the welcome of its epoch — fail with
 * [IncomingPacketDeferredException]: their envelope is parked and processed again later.
 */
@Suppress("LongParameterList")
class RouteIncomingPacketWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val resolveDirectPeerAccess: ResolveDirectPeerAccessUseCase,
    private val directConversationRepository: DirectConversationRepository,
    private val resolveControlConversationId: ResolveControlConversationIdUseCase,
    private val checkGroupPacketAccepted: CheckGroupPacketAcceptedUseCase,
    private val checkGroupPacketReady: CheckGroupPacketReadyUseCase,
    private val groupPacketDispatcher: GroupPacketDispatcher,
    private val controlPacketDispatcher: ControlPacketDispatcher
) : WorkflowStateMachine<
        IncomingCommand.RouteIncomingPacket,
        RouteIncomingPacketWorkflow.Step,
        RouteIncomingPacketWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.RouteIncomingPacket> = IncomingCommand.RouteIncomingPacket::class

    override fun firstStep(command: IncomingCommand.RouteIncomingPacket): Step {
        val packet = command.packet
        val groupId = packet.groupIdOrNull()
        return when {
            packet is ChatMessagePacket || packet is OperationMessagePacket -> Step.CheckDirectAuthorization(command)
            groupId != null -> Step.CheckGroupAccepted(groupId, command)
            packet is DeliveryReceiptPacket || packet is ReadReceiptPacket -> Step.ApplyReceipt(command)
            else -> Step.FindControlConversation(command)
        }
    }

    override suspend fun perform(step: Step): Outcome {
        val command = step.command
        return when (step) {
            is Step.CheckDirectAuthorization -> checkDirectAuthorization(command)
            is Step.EnsureDirectConversation ->
                Outcome.DirectConversationResolved(
                    directConversationRepository.getOrCreate(command.contactId).getOrThrow()
                )
            is Step.ReceiveDirectMessage -> {
                engine.run(
                    IncomingCommand.ReceiveDirectMessage(command.toContext(step.conversationId), step.packet)
                ).getOrThrow()
                Outcome.Handled
            }
            is Step.ApplyDirectOperation -> {
                IncomingDirectTransportPolicy.checkOperationAllowed(command.transportMode)
                engine.run(
                    IncomingCommand.ApplyRemoteMessageOperation(
                        conversationId = step.conversationId,
                        senderContactId = command.contactId,
                        operation = step.packet.message.operation,
                        conversation = OperationConversation.DIRECT,
                        sentAtEpochMilliseconds = command.receivedAtEpochMilliseconds
                    )
                ).getOrThrow()
                Outcome.Handled
            }
            is Step.CheckGroupAccepted -> checkGroupAccepted(step.groupId, command)
            is Step.CheckGroupReady -> checkGroupReady(step.groupId, command)
            is Step.DispatchGroupPacket -> {
                groupPacketDispatcher.dispatch(
                    ReceivedPacket(command.toContext(step.groupId), command.packet)
                ).getOrThrow()
                Outcome.Handled
            }
            is Step.ApplyReceipt -> {
                engine.run(command.toDeliveryEvent()).getOrThrow()
                Outcome.Handled
            }
            is Step.FindControlConversation ->
                Outcome.ControlConversationResolved(
                    resolveControlConversationId(command.contactId, command.packet.packetId).getOrThrow()
                )
            is Step.DispatchControlPacket -> {
                controlPacketDispatcher.dispatch(
                    ReceivedPacket(command.toContext(step.conversationId), command.packet)
                ).getOrThrow()
                Outcome.Handled
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.DirectAuthorized -> Transition.Next(Step.EnsureDirectConversation(step.command))
            is Outcome.DirectConversationResolved ->
                when (val packet = step.command.packet) {
                    is ChatMessagePacket ->
                        Transition.Next(Step.ReceiveDirectMessage(outcome.conversationId, packet, step.command))
                    is OperationMessagePacket ->
                        Transition.Next(Step.ApplyDirectOperation(outcome.conversationId, packet, step.command))
                    else -> Transition.Reject.because(IllegalStateException("Packet is not a direct message"))
                }
            is Outcome.ControlConversationResolved ->
                Transition.Next(Step.DispatchControlPacket(outcome.conversationId, step.command))
            is Outcome.GroupAccepted -> Transition.Next(Step.CheckGroupReady(outcome.groupId, step.command))
            is Outcome.GroupReady -> Transition.Next(Step.DispatchGroupPacket(outcome.groupId, step.command))
            is Outcome.Deferred -> Transition.Reject.because(IncomingPacketDeferredException(outcome.wait))
            Outcome.Dropped,
            Outcome.Handled -> Transition.Complete()
        }

    private suspend fun checkDirectAuthorization(command: IncomingCommand.RouteIncomingPacket): Outcome =
        when (resolveDirectPeerAccess(command.contactId).getOrThrow()) {
            DirectPeerAccess.AUTHORIZED -> Outcome.DirectAuthorized
            DirectPeerAccess.PENDING -> Outcome.Deferred(DeferredPacketWait.ContactExchange(command.contactId))
            DirectPeerAccess.DROPPED -> Outcome.Dropped
        }

    private suspend fun checkGroupAccepted(groupId: String, command: IncomingCommand.RouteIncomingPacket): Outcome =
        if (checkGroupPacketAccepted(groupId, command.packet).getOrThrow()) {
            Outcome.GroupAccepted(groupId)
        } else {
            Outcome.Dropped
        }

    private suspend fun checkGroupReady(groupId: String, command: IncomingCommand.RouteIncomingPacket): Outcome =
        checkGroupPacketReady(groupId, command.packet).getOrThrow()
            ?.let(Outcome::Deferred)
            ?: Outcome.GroupReady(groupId)

    private fun IncomingCommand.RouteIncomingPacket.toContext(conversationId: String): IncomingPacketContext =
        IncomingPacketContext(
            contactId = contactId,
            conversationId = conversationId,
            encodedTransportPayload = encodedTransportPayload,
            transportMode = transportMode,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )

    private fun IncomingCommand.RouteIncomingPacket.toDeliveryEvent(): DeliveryCommand.ApplyDeliveryEvent =
        when (val receipt: SparrowPacket = packet) {
            is DeliveryReceiptPacket ->
                DeliveryCommand.ApplyDeliveryEvent(
                    DeliveryEventTarget.Recipient(receipt.messageId, contactId),
                    MessageDeliveryEvent.DELIVERY_CONFIRMED
                )
            is ReadReceiptPacket ->
                DeliveryCommand.ApplyDeliveryEvent(
                    DeliveryEventTarget.Recipient(receipt.messageId, contactId),
                    MessageDeliveryEvent.READ_CONFIRMED
                )
            else -> error("Packet is not a receipt")
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.RouteIncomingPacket

        data class CheckDirectAuthorization(
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        data class EnsureDirectConversation(
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        data class ReceiveDirectMessage(
            val conversationId: String,
            val packet: ChatMessagePacket,
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        data class ApplyDirectOperation(
            val conversationId: String,
            val packet: OperationMessagePacket,
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        data class CheckGroupAccepted(
            val groupId: String,
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        /** A group packet waits until the welcome of its epoch is installed. */
        data class CheckGroupReady(
            val groupId: String,
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        data class DispatchGroupPacket(
            val groupId: String,
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        data class ApplyReceipt(
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        data class FindControlConversation(
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step

        data class DispatchControlPacket(
            val conversationId: String,
            override val command: IncomingCommand.RouteIncomingPacket
        ) : Step
    }

    sealed interface Outcome {
        data object DirectAuthorized : Outcome

        data class DirectConversationResolved(
            val conversationId: String
        ) : Outcome

        /** Identity and invitation packets use the contact's chat, or a control ID when none exists. */
        data class ControlConversationResolved(
            val conversationId: String
        ) : Outcome

        data class GroupAccepted(
            val groupId: String
        ) : Outcome

        data class GroupReady(
            val groupId: String
        ) : Outcome

        /** Arrived too early: the envelope is parked until [wait] is met. */
        data class Deferred(
            val wait: DeferredPacketWait
        ) : Outcome

        /** Not authorized, or the group was deleted locally: the packet is acknowledged without effect. */
        data object Dropped : Outcome

        data object Handled : Outcome
    }
}
