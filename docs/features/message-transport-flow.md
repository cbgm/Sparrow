# Message, invitation and transport flow

This page connects the current client layers from UI to encrypted network delivery.

## Direct send

```mermaid
sequenceDiagram
    participant VM as DirectConversationActionsController
    participant ORCH as ConversationOrchestrator
    participant WF as SendDirectMessageWorkflow
    participant AUTH as RequireDirectChatAuthorizationUseCase
    participant DEL as DeliverDirectMessageWorkflow
    participant DB as Room
    participant OUT as ProtocolOutbox
    participant RUN as DefaultOutboxRunner
    participant SEND as OutgoingPacketSender
    participant WS as DefaultWebSocketTransportClient

    VM->>ORCH: awaitResult(DirectConversationCommand.SendMessage)
    ORCH->>WF: blocked? pending identity change?
    WF->>AUTH: CheckAuthorization step
    alt authorized
        WF->>DEL: DeliverMessage step
        DEL->>DB: persist message
        DEL->>OUT: enqueue encrypted packet work
        RUN->>SEND: process persisted packet
        SEND->>WS: send
    else not authorized
        WF->>DB: persist WAITING_FOR_AUTHORIZATION (optionally request reauthorization)
    end
```

A queued Direct message is released by `ReleaseWaitingDirectMessagesWorkflow` (via `ActivateAuthorizedConversationWorkflow`) only after orchestration establishes fresh authorization. There is no plaintext fallback.

## Incoming delivery

```mermaid
sequenceDiagram
    participant WS as transport incoming gateway
    participant IR as DefaultIncomingEnvelopeRunner
    participant IP as IncomingEnvelopeProcessorImpl
    participant H as IncomingTransportMessageHandler
    participant ROUTE as RouteIncomingPacketWorkflow
    participant CTRL as ControlPacketDispatcher
    participant GRP as GroupPacketDispatcher

    WS-->>IR: envelope
    IR->>IP: process
    IP->>H: decrypt/decode packet
    H->>ROUTE: IncomingPacket
    ROUTE->>ROUTE: direct message: ReceiveDirectMessageWorkflow / receipt: ApplyDeliveryEventWorkflow
    ROUTE->>GRP: group packets -> ReceiveGroup*Workflow
    ROUTE->>CTRL: identity/invite packets -> ReceiveContact*/ReceiveGroupInvite*Workflow
```

## Direct invitation/authorization

The generic invitation row is owned by `:feature:invite`, the cryptographic exchange by `:feature:identity`, and the cross-feature sequence by the lifecycle workflows (`StartDirectInvitationWorkflow`, `HandleDirectInvitationResultWorkflow`, …) run by `OpenDirectConversationWorkflow` and `InvitationResultObserver`.

```mermaid
flowchart LR
    CONTACT[contact selection] --> OPEN[OpenDirectConversationWorkflow]
    OPEN --> FLOW[StartDirectInvitationWorkflow]
    FLOW --> ID[Identity exchange]
    FLOW --> INV[Invitation lifecycle]
    INV --> OBS[InvitationResultObserver]
    OBS --> FLOW2[HandleDirectInvitationResultWorkflow /<br/>RecoverAcceptedDirectInvitationWorkflow]
    FLOW2 --> CHAT[ActivateAuthorizedConversationWorkflow]
    CHAT --> REL[ReleaseWaitingDirectMessagesWorkflow]
```

Explicit recovery/reconnection is documented separately in [Identity recovery](identity-recovery.md).

## Group send

`SendGroupMessageWorkflow` validates active membership, obtains current recipients from Membership, encrypts for the current Group epoch and queues one packet per active recipient (`CheckMembership -> FindRecipients -> [FlushQueuedMessages] -> StoreMessage -> EncryptMessage -> SaveRecipientStates -> EnqueuePackets`). Group security state is not inferred from the invitation table.

## Offline mailbox

When direct online routing is unavailable or policy selects mailbox delivery, `OutgoingPacketSender` uses the recipient mailbox route/capability resolved by orchestration/transport. Client mailbox lifecycle is implemented around `DefaultMailboxCoordinator`, `DefaultMailboxCapabilityLifecycle`, `MailboxRouteProvisioner`, `MailboxPendingSynchronizer` and `HttpMailboxGateway`.

`MailboxRouteProvisioner` decides from the freshly verified, signed node directory, never from a socket failure:

- **Moved node:** a node keeps its node ID when its address changes (for example a new public IP). Saved mailbox credentials whose node ID is in the directory under a new address switch to the node's new mailbox access endpoint. A mailbox whose delivery route still points to the old address is renewed and the new route is shared with the contact (`MailboxRoutePacket`), because contacts keep delivering to the old address until then.
- **Retired node:** a node ID that is missing from the verified directory means the old mailbox is gone; a replacement is saved without revoking the old one.
- **Unreachable node:** `DefaultMailboxCapabilityLifecycle.revokeForContact` deletes the remote route and marks the local mailbox revocation pending. If the node cannot be reached (recoverable connectivity failure), the revocation stays pending instead of failing the caller (for example approving an identity change); provisioning retries pending revocations later.

The server `mailbox` service stores opaque encrypted envelopes behind recipient-selected capabilities. Push is a wake-up path, not decryption/interpretation by the server.

## Delivery/read receipts

Protocol receipts are routed by `RouteIncomingPacketWorkflow` to `ApplyDeliveryEventWorkflow`; outbox progress reaches the same workflow through `OutboxDeliveryStateRouter`. Direct and Group maintain distinct recipient/delivery state logic (`DirectMessageDeliveryStateMachine`, `GroupMessageDeliveryStateMachine`, `DirectMessageDeliveryRepository` and `GroupRecipientDeliveryRepository` in `:feature:conversationorchestration:delivery`), while generic outbox execution remains in `:feature:messaging`.
