# Message, invitation and transport flow

This page connects the current client layers from UI to encrypted network delivery.

## Direct send

```mermaid
sequenceDiagram
    participant VM as DirectConversationViewModel
    participant UC as SendOrQueueDirectMessageUseCase
    participant DP as DirectOutgoingMessageProcessor
    participant AUTH as RequireDirectChatAuthorizationUseCase
    participant DB as Room
    participant OUT as ProtocolOutbox
    participant RUN as DefaultOutboxRunner
    participant SEND as OutgoingPacketSender
    participant WS as DefaultWebSocketTransportClient

    VM->>UC: send
    UC->>DP: send or queue
    DP->>AUTH: authorization check
    alt authorized
        DP->>DB: persist message
        DP->>OUT: enqueue encrypted packet work
        RUN->>SEND: process persisted packet
        SEND->>WS: send
    else not authorized
        DP->>DB: persist WAITING_FOR_AUTHORIZATION
    end
```

A queued Direct message is released with `DirectOutgoingMessageProcessor.releaseWaitingForAuthorization()` only after orchestration establishes fresh authorization. There is no plaintext fallback.

## Incoming delivery

```mermaid
sequenceDiagram
    participant WS as transport incoming gateway
    participant IR as DefaultIncomingEnvelopeRunner
    participant IP as DefaultIncomingEnvelopeProcessor
    participant PROTO as DefaultProtocolPacketHandler
    participant FLOW as ConversationFlowHandler
    participant ROUTER as IncomingPacketRouter

    WS-->>IR: envelope
    IR->>IP: process
    IP->>PROTO: decode/verify packet
    PROTO->>FLOW: identity/invite/membership protocol packets
    PROTO->>ROUTER: chat/message/receipt packets
```

## Direct invitation/authorization

The generic invitation row is owned by `:feature:invite`, the cryptographic exchange by `:feature:identity`, and the cross-feature sequence by `ConversationFlowHandler`/`InvitationResultObserver`.

```mermaid
flowchart LR
    CONTACT[contact selection] --> FLOW[ConversationFlowHandler.startDirectInvitation]
    FLOW --> ID[Identity exchange]
    FLOW --> INV[Invitation lifecycle]
    INV --> OBS[InvitationResultObserver]
    OBS --> FLOW2[recover/accept result]
    FLOW2 --> CHAT[authorized Direct conversation]
    CHAT --> REL[release waiting messages]
```

Explicit recovery/reconnection is documented separately in [Identity recovery](identity-recovery.md).

## Group send

`GroupOutgoingMessageProcessor` validates active membership, obtains current routing members from Membership, encrypts for the current Group epoch and queues one packet per active recipient. Group security state is not inferred from the invitation table.

## Offline mailbox

When direct online routing is unavailable or policy selects mailbox delivery, `OutgoingPacketSender` uses the recipient mailbox route/capability resolved by orchestration/transport. Client mailbox lifecycle is implemented around `DefaultMailboxCoordinator`, `DefaultMailboxCapabilityLifecycle`, `MailboxRouteProvisioner`, `MailboxPendingSynchronizer` and `HttpMailboxGateway`.

The server `mailbox` service stores opaque encrypted envelopes behind recipient-selected capabilities. Push is a wake-up path, not decryption/interpretation by the server.

## Delivery/read receipts

Protocol receipts are routed back into Chats. Direct and Group maintain distinct recipient/delivery state logic (`DirectOutboxDeliveryHandler`, `GroupOutboxDeliveryHandler` and their data paths), while generic outbox execution remains in `:feature:messaging`.
