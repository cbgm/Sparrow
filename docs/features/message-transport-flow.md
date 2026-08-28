# Message transport flow

This page follows the current message path through the client and server classes that actually participate in delivery.

## Direct message: client A to client B

```mermaid
sequenceDiagram
    autonumber
    participant UI as DirectViewModel
    participant UC as SendDirectMessageUseCase
    participant Repo as DirectMessageRepositoryImpl
    participant DOP as DirectOutgoingMessageProcessor
    participant A as MessageAttachmentDataSource
    participant PO as ProtocolOutbox
    participant OR as DefaultOutboxRunner
    participant OP as DefaultOutboxProcessor
    participant PS as OutgoingPacketSender
    participant PF as OutgoingTransportPayloadFactory
    participant RR as OutgoingRecipientRoutingResolver
    participant WS as WebSocketOutgoingWireSender
    participant WSC as DefaultWebSocketTransportClient
    participant GA as Gateway A
    participant FA as Federation A
    participant FB as Federation B
    participant GB as Gateway B
    participant IG as WebSocketIncomingEnvelopeGateway
    participant IR as DefaultIncomingEnvelopeRunner
    participant IP as DefaultIncomingEnvelopeProcessor
    participant CP as IncomingPacketProcessor

    UI->>UC: send text + outgoing attachments
    UC->>Repo: send(...)
    Repo->>DOP: create/store/enqueue
    DOP->>A: prepareAttachments(...) when present
    A-->>DOP: prepared blob metadata/references
    DOP->>DOP: persist local message as QUEUED
    DOP->>PO: enqueue protocol packet
    OR->>OP: processPending()
    OP->>PS: send(outbox item)
    PS->>PF: create protected transport payload
    PS->>RR: resolve recipient routing ID
    PS->>WS: sendWithAcceptance(...)
    WS->>WSC: send envelope
    WSC->>GA: WebSocket /v1/gateway
    GA->>FA: forward when recipient is remote
    FA->>FB: signed federated envelope
    FB->>GB: destination-node delivery
    GB-->>WSC: recipient WebSocket envelope
    WSC-->>IG: incoming envelope flow
    IG-->>IR: IncomingTransportEnvelope
    IR->>IP: process envelope
    IP->>CP: decrypted transport payload via IncomingMessageHandler
    CP->>CP: PacketCodec.decode + IncomingPacketRouter
```

If both clients are reachable through the same Community Node, the server can deliver locally without the cross-node federation hop.

## Direct outgoing classes

The Direct conversation path remains deliberately separate from Group messaging:

```text
DirectViewModel
  -> SendDirectMessageUseCase
  -> DirectMessageRepository
  -> DirectMessageRepositoryImpl
  -> DirectOutgoingMessageProcessor
  -> ProtocolOutbox
```

`DirectOutgoingMessageProcessor` validates message content/authorization, prepares attachment blobs when present, persists the local outgoing message, creates the `ChatMessagePacket`, links its packet ID and queues it in `ProtocolOutbox`.

The shared outbox runtime then continues with:

```text
DefaultOutboxRunner
  -> DefaultOutboxProcessor
  -> OutgoingPacketSender
      -> OutgoingTransportPayloadFactory
      -> OutgoingRecipientRoutingResolver
      -> OutgoingWireSender / WebSocketOutgoingWireSender
  -> DefaultWebSocketTransportClient
```

`DefaultOutboxProcessor` owns persistent outbox state transitions and batching. `OutgoingPacketSender` decodes the already-created application packet, asks `OutgoingTransportPayloadFactory` for the required transport protection, resolves the destination with `OutgoingRecipientRoutingResolver`, then sends the encoded transport payload through `OutgoingWireSender`.

The outbox processes different recipient groups concurrently (up to eight recipient groups), while preserving ordering within one recipient group.

## Direct authorization / re-invitation

A Direct composer can be `READY`, `REINVITE_REQUIRED`, `REINVITE_PENDING` or `DISABLED`.

If authorization is missing but automatic re-invitation is allowed, `DirectViewModel` uses `QueueDirectMessageUntilAuthorizedUseCase` instead of sending the packet immediately. `DirectOutgoingMessageProcessor.queueUntilAuthorized(...)` prepares any attachments and persists the message with `WAITING_FOR_AUTHORIZATION`.

- acceptance calls `HandleAcceptedDirectInvitationUseCase`, which releases still-valid waiting messages back to `QUEUED` and enqueues their packets;
- decline calls `HandleDeclinedDirectInvitationUseCase`, which discards waiting messages;
- `DirectPendingAuthorizationMessagePolicy` expires waiting messages after two days.

Current-location and contact attachment actions use the same authorization behavior; they are sent as attachment-only messages with empty text.

## Attachment preparation and loading

When a Direct or Group message contains attachments, its outgoing processor asks `MessageAttachmentDataSource.prepareAttachments(...)` to prepare/upload them before the final chat packet is queued. The attachment feature owns the blob pipeline; chats receives prepared protocol attachment metadata/references.

```mermaid
sequenceDiagram
    participant Chat as Direct/Group outgoing processor
    participant A as MessageAttachmentDataSource
    participant B as BlobTransferDataSource
    participant N as Community Node blob endpoint
    participant O as ProtocolOutbox

    Chat->>A: prepareAttachments(outgoing attachments)
    A->>B: encrypt/upload each blob
    B->>N: upload encrypted blob
    N-->>B: blob reference
    B-->>A: prepared attachment metadata
    A-->>Chat: PreparedMessageAttachmentDto list
    Chat->>O: enqueue chat packet with blob references
```

Image, video, file, location and contact currently use this blob path. Incoming attachment metadata is stored with the message; bytes are loaded/cached through `:feature:attachments` when needed. Incoming image/video/file attachments can also get saved conversation copies, while location/contact blobs are excluded from the saved media/files tree.

## Group outgoing classes

Group messages intentionally use a separate conversation path:

```text
GroupViewModel
  -> SendGroupMessageUseCase
  -> GroupMessageRepository
  -> GroupMessageRepositoryImpl
  -> GroupOutgoingMessageProcessor
  -> ProtocolOutbox
```

`GroupOutgoingMessageProcessor`:

1. validates message content and active membership;
2. obtains current security-epoch recipients;
3. prepares attachment blobs when present;
4. encodes `GroupMessageContent` and encrypts it through `GroupSecurityManager`;
5. creates one `GroupChatMessagePacket` per active recipient;
6. stores one `MessageRecipientStateEntity` per recipient;
7. enqueues each packet separately.

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

Only current active members are selected from the current security epoch. A removed member is not retained as a normal recipient merely because that contact belonged to the group previously.

## Transport protection and routing

`OutgoingTransportPayloadFactory` applies `OutgoingPacketTransportPolicy` and encrypts through `TransportMessageCipher` when the packet/contact state requires it. Some bootstrap/invitation packets must be routable before mutual identity material exists, so the policy explicitly handles those exceptions.

`OutgoingRecipientRoutingResolver` chooses bootstrap/direct/group routing based on the concrete `SparrowPacket`, using `ContactRoutingDataSource` and `GroupRoutingDataSource`.

`WebSocketOutgoingWireSender` then selects the local sender routing ID (`LocalRoutingIdProvider` or `LocalBootstrapRoutingIdProvider`), chooses a known recipient mailbox route when one is available, and sends either a normal `TransportEnvelope` or `FederatedEnvelope` through the WebSocket client.

## Incoming client boundary

```mermaid
sequenceDiagram
    participant WS as DefaultWebSocketTransportClient
    participant G as WebSocketIncomingEnvelopeGateway
    participant R as DefaultIncomingEnvelopeRunner
    participant P as DefaultIncomingEnvelopeProcessor
    participant C as IncomingPacketProcessor
    participant Router as IncomingPacketRouter
    participant D as DirectIncomingPacketProcessor
    participant GR as GroupIncomingPacketProcessor

    WS-->>G: incoming envelope flow
    G-->>R: IncomingTransportEnvelope
    R->>P: process(envelope)
    P->>P: resolve sender contact + local encryption keys
    P->>C: IncomingMessageHandler.handle(...)
    C->>C: decrypt transport + PacketCodec.decode()
    C->>Router: DecodedIncomingPacketDto
    Router->>D: Direct packet
    Router->>GR: Group packet
    R-->>G: acknowledge processed/rejected envelope
    G-->>WS: acknowledgeIncomingEnvelope(...)
```

`DefaultIncomingEnvelopeProcessor` also reconciles known contact routing after a successfully processed envelope. Permanently rejected envelopes are acknowledged and discarded; unknown-sender envelopes are not acknowledged by `DefaultIncomingEnvelopeRunner`.

## Offline recipient

When the recipient is not currently reachable through the live route, federation can use a recipient-selected mailbox route and store the encrypted/federated envelope in that mailbox. `MailboxPushNotifier` asks the push service for a wake-up. Android FCM wakes the app/background worker, and `SynchronizePendingMessagesUseCase` processes push pending envelopes and also asks `MailboxCoordinator` to synchronize mailbox-delivered envelopes.

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
    A->>A: SynchronizePendingMessagesUseCase
    A->>M: synchronize pending mailbox data
    A->>A: decrypt + process locally
    A->>M: acknowledge processed envelope
```

Push remains a wake-up mechanism; the server does not need conversation plaintext to provide offline delivery.

## Receipts

`DeliveryReceiptPacket` and `ReadReceiptPacket` use shared protocol formats, but `ReceiptIncomingPacketRouter` dispatches them to Direct or Group-specific receipt handling. Group receipt processing updates recipient-specific state; Direct receipt processing updates Direct message state.

## Typing

Typing is ephemeral and intentionally separate from durable message delivery. The WebSocket/gateway/federation path can route typing events, but typing is not stored in message history or recovered from mailbox history.
