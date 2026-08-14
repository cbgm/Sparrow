# Message transport flow

This page follows a message through the actual classes in the current codebase.

## Direct message: client A to client B

```mermaid
sequenceDiagram
    autonumber
    participant UI as DirectViewModel
    participant UC as SendDirectMessageUseCase
    participant Repo as DirectMessageRepositoryImpl
    participant DOP as DirectOutgoingMessageProcessor
    participant PO as ProtocolOutbox
    participant OR as DefaultOutboxRunner
    participant OP as DefaultOutboxProcessor
    participant WS as DefaultWebSocketTransportClient
    participant GA as Gateway A
    participant FA as Federation A
    participant FB as Federation B
    participant GB as Gateway B
    participant IN as DefaultIncomingEnvelopeRunner
    participant IP as IncomingPacketProcessor

    UI->>UC: send message
    UC->>Repo: send
    Repo->>DOP: create ChatMessagePacket
    DOP->>DOP: persist local message as QUEUED
    DOP->>PO: enqueue packet
    OR->>OP: process pending outbox
    OP->>OP: packet encode + transport encryption + routing ID
    OP->>WS: send opaque transport payload
    WS->>GA: WebSocket /v1/gateway
    GA->>FA: recipient not local
    FA->>FB: signed federated envelope
    FB->>GB: destination gateway delivery
    GB-->>IN: recipient WebSocket envelope
    IN->>IP: decrypt/decode/route
    IP-->>UI: stored incoming Direct message
```

If both clients are on the same Community Node, federation can deliver locally without the cross-node hop.

## Direct outgoing classes

The Direct path is:

```text
DirectViewModel
  -> SendDirectMessageUseCase
  -> DirectMessageRepository
  -> DirectMessageRepositoryImpl
  -> DirectOutgoingMessageProcessor
  -> ProtocolOutbox
```

`DirectOutgoingMessageProcessor` creates a `ChatMessagePacket`, persists the local message, and queues the packet. Delivery transitions later pass through `DirectMessageDeliveryCoordinator` and `DirectMessageDeliveryStateMachine`.

## Group outgoing classes

Group messages intentionally use a separate path:

```text
GroupViewModel
  -> SendGroupMessageUseCase
  -> GroupMessageRepository
  -> GroupMessageRepositoryImpl
  -> GroupOutgoingMessageProcessor
  -> ProtocolOutbox
```

`GroupOutgoingMessageProcessor`:

1. verifies the local membership is active;
2. obtains the current security epoch and active recipients;
3. encrypts the group payload through `GroupSecurityManager`/`SodiumGroupCrypto`;
4. creates one `GroupChatMessagePacket` per active recipient;
5. stores one `MessageRecipientStateEntity` per recipient;
6. enqueues each packet separately.

This is why Group delivered/read state is derived from per-recipient state rather than Direct-chat state.

```mermaid
flowchart LR
    GM[Group message] --> R1[Recipient state A]
    GM --> R2[Recipient state B]
    GM --> R3[Recipient state C]
    R1 --> AGG[GroupMessageDeliveryStateMachine]
    R2 --> AGG
    R3 --> AGG
    AGG --> UI[aggregate sent/delivered/read progress]
```

Only current active members are selected from the current security epoch. A removed member is not simply kept as a normal recipient and later given missed history.

## Transport protection before the wire

`DefaultOutboxProcessor` asks `OutgoingTransportPayloadFactory` for a transport payload. The default implementation applies `OutgoingPacketTransportPolicy` and, where required, encrypts through `TransportMessageCipher`.

Some bootstrap/invitation packets must be deliverable before the peers have enough identity material for the normal encrypted channel; the policy explicitly defines those exceptions. Normal message/security/verification traffic uses the strongest applicable identity/group transport path.

## Offline recipient

When the recipient is not currently connected, federation can store the **encrypted/federated envelope** in the recipient-selected mailbox. `MailboxPushNotifier` asks the push service for a wake-up. On Android, FCM wakes the app/background worker, which retrieves/processes/acknowledges the pending mailbox envelope.

```mermaid
sequenceDiagram
    participant F as FederationRouter
    participant M as Mailbox
    participant P as PushCoordinator
    participant FCM as Firebase Cloud Messaging
    participant A as Android client

    F->>M: store opaque encrypted envelope
    M->>P: wake-up request
    P->>FCM: send opaque wake-up identifier
    FCM-->>A: wake app
    A->>M: authenticated mailbox fetch
    A->>A: decrypt + process packet locally
    A->>M: acknowledge/remove envelope
```

The mailbox server does not need conversation plaintext to provide offline delivery.

## Receipts

`DeliveryReceiptPacket` and `ReadReceiptPacket` use the shared protocol format, but `ReceiptIncomingPacketRouter` sends them to Direct or Group-specific receipt handling. Group receipt processing updates the recipient-specific state; Direct receipt processing updates the Direct message state.

## Typing

Typing is ephemeral and is intentionally not the same as durable message delivery. The WebSocket/gateway/federation path may route typing events between nodes, but they are not normal chat messages, are not stored in the message history, and are not delivered from mailbox history.
