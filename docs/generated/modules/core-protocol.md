# `:core:protocol`

Source directory: `core/protocol`

## Direct project dependencies

- `:core`
- `:core:crypto`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `EncryptedBlobReference` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/MessageAttachment.kt` |
| `GroupPinnedAttachmentProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/GroupPinnedAttachmentProvider.kt` |
| `MessageAttachment` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/MessageAttachment.kt` |
| `MessageAttachmentConstraints` | `object` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/MessageAttachmentConstraints.kt` |
| `MessageAttachmentType` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/attachment/MessageAttachment.kt` |
| `DirectChatAuthorizationRevocationProtocol` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/authorization/DirectChatAuthorizationRevocationProtocol.kt` |
| `DirectChatAuthorizationRevocationSender` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/authorization/DirectChatAuthorizationRevocationSender.kt` |
| `GroupAvatarMetadata` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/avatar/GroupAvatarMetadata.kt` |
| `GroupAvatarPayload` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/avatar/GroupAvatarMetadata.kt` |
| `GroupAvatarProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/avatar/GroupAvatarProvider.kt` |
| `GroupAvatarSnapshot` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/avatar/GroupAvatarProvider.kt` |
| `KotlinxPacketCodec` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/codec/KotlinxPacketCodec.kt` |
| `PacketCodec` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/codec/PacketCodec.kt` |
| `DefaultProtocolPacketHandler` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/DefaultProtocolPacketHandler.kt` |
| `IncomingMessageHandler` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/IncomingMessageHandler.kt` |
| `IncomingMessageRejectedException` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/IncomingMessageRejectedException.kt` |
| `IncomingPacketContext` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/IncomingPacketContext.kt` |
| `ProtocolPacketHandler` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/ProtocolPacketHandler.kt` |
| `TypedProtocolPacketHandler` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/handler/TypeProtocolHandler.kt` |
| `LocalEncryptionKeyPair` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalEncryptionKeyPair.kt` |
| `LocalEncryptionKeyPairProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalEncryptionKeyPairProvider.kt` |
| `LocalIdentityChangeHandler` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalIdentityChangeHandler.kt` |
| `LocalIdentityUnavailableException` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalIdentityUnavailableException.kt` |
| `LocalPublicIdentity` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalPublicIdentity.kt` |
| `LocalPublicIdentityProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalPublicIdentityProvider.kt` |
| `LocalSigningKeyPair` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalSigningKeyPair.kt` |
| `LocalSigningKeyPairProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalSigningKeyPairProvider.kt` |
| `LocalSigningPublicKeyProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/identity/LocalSigningPublicKeyProvider.kt` |
| `ContactInvitationDeclineProtocol` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/invitation/ContactInvitationDeclineProtocol.kt` |
| `ContactInvitationHandshakeProtocol` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/invitation/ContactInvitationHandshakeProtocol.kt` |
| `ContactInvitationPayloadEncoder` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/invitation/ContactInvitationPayloadEncoder.kt` |
| `ContactInvitationRequestProtocol` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/invitation/ContactInvitationRequestProtocol.kt` |
| `LocalMailboxCredential` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `MailboxCapabilityLifecycle` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `MailboxDeliveryRoute` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `MailboxRouteRepository` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `NoOpMailboxCapabilityLifecycle` | `object` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/mailbox/MailboxModels.kt` |
| `GroupMessageContent` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/GroupMessageContent.kt` |
| `GroupMessageContentCodec` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/GroupMessageContent.kt` |
| `MessageDeletionPayload` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageDeletionPayload.kt` |
| `MessageDeletionPayloadCodec` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageDeletionPayloadCodec.kt` |
| `MessageEditPayload` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageEditPayload.kt` |
| `MessageEditPayloadCodec` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageEditPayloadCodec.kt` |
| `MessageReactionPayload` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/message/MessageReactionPayload.kt` |
| `OutboxDeliveryStateListener` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxDeliveryStateListener.kt` |
| `OutboxEvent` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxEvent.kt` |
| `OutboxProcessingResult` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxProcessor.kt` |
| `OutboxProcessor` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxProcessor.kt` |
| `OutboxRunner` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxRunner.kt` |
| `OutboxStateMachine` | `object` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxStateMachine.kt` |
| `OutboxStatus` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/OutboxStatus.kt` |
| `ProtocolOutbox` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/ProtocolOutbox.kt` |
| `ProtocolOutboxFailureEvent` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/ProtocolOutboxFailureEvent.kt` |
| `ProtocolOutboxItem` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/outbox/ProtocolOutboxItem.kt` |
| `ChatMessagePacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ChatMessagePacket.kt` |
| `ContactInviteAcceptedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `ContactInviteDeclinedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `ContactInvitePacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `ContactReadyPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `ContactVerificationReceiptPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `DeliveryReceiptPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/DeliveryReceiptPacket.kt` |
| `DirectChatAuthorizationRevokedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ContactInvitationPackets.kt` |
| `GroupAvatarUpdatedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupAvatarUpdatedPacket.kt` |
| `GroupChatMessagePacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupChatMessagePacket.kt` |
| `GroupConversationDeletedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupConversationDeletedPacket.kt` |
| `GroupCreatedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupCreatedPacket.kt` |
| `GroupDescriptionUpdatedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupDescriptionUpdatedPacket.kt` |
| `GroupInviteDeclinedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupInviteDeclinedPacket.kt` |
| `GroupInvitePacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupInvitePacket.kt` |
| `GroupInviteReceivedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupInviteReceivedPacket.kt` |
| `GroupJoinRequestPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupJoinRequestPacket.kt` |
| `GroupLeaveRequestPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupLeaveRequestPacket.kt` |
| `GroupMemberActivatedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMemberActivatedPacket.kt` |
| `GroupMemberActivationAcknowledgementPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMemberActivationAcknowledgementPacket.kt` |
| `GroupMemberPayload` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMemberPayload.kt` |
| `GroupMemberRemovedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMemberRemovedPacket.kt` |
| `GroupMembershipChangePayload` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupCreatedPacket.kt` |
| `GroupMessageDeletionPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMessageDeletionPacket.kt` |
| `GroupMessageEditPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupMessageEditPacket.kt` |
| `GroupPinUpdatedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupPinUpdatedPacket.kt` |
| `GroupProtocolPayloadEncoder` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupProtocolPayloadEncoder.kt` |
| `GroupReadyAcknowledgementPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupReadyAcknowledgementPacket.kt` |
| `GroupTitleUpdatedPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupTitleUpdatedPacket.kt` |
| `GroupVerificationMemberPayload` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationReceiptPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationSnapshotPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupVerificationPackets.kt` |
| `GroupVerificationSnapshotRequestPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/GroupVerificationPackets.kt` |
| `IdentityAcknowledgementPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/IdentityAcknowledgementPacket.kt` |
| `IdentityPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/IdentityPacket.kt` |
| `MailboxRoutePacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/MailboxRoutePacket.kt` |
| `MessageDeletionPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/MessageDeletionPacket.kt` |
| `MessageEditPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/MessageEditPacket.kt` |
| `ReadReceiptPacket` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/ReadReceiptPacket.kt` |
| `SparrowPacket` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/packet/SparrowPacket.kt` |
| `DefaultPhoneNumberNormalizer` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/phone/DefaultPhoneNumberNormalizer.kt` |
| `LocalPhoneNumberProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/phone/LocalPhoneNumberProvider.kt` |
| `PhoneNumberNormalizer` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/phone/PhoneNumberNormalizer.kt` |
| `LocalProfilePictureMetadataProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/LocalProfilePictureMetadataProvider.kt` |
| `LocalProfilePictureProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/LocalProfilePictureProvider.kt` |
| `LocalProfilePictureSnapshot` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/LocalProfilePictureProvider.kt` |
| `ProfilePictureMetadata` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/ProfilePictureMetadata.kt` |
| `ProfilePicturePayload` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/ProfilePictureMetadata.kt` |
| `RemoteProfilePictureMetadataProcessor` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/RemoteProfilePictureMetadataProcessor.kt` |
| `RemoteProfilePictureProvider` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/RemoteProfilePictureProvider.kt` |
| `RemoteProfilePictureSnapshot` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/profile/RemoteProfilePictureProvider.kt` |
| `ByteArrayAsBase64Serializer` | `object` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/serializer/ByteArrayAsBase64Serializer.kt` |
| `OutgoingWireAcceptance` | `class` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/transport/OutgoingWireSender.kt` |
| `OutgoingWireSender` | `interface` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/transport/OutgoingWireSender.kt` |
| `ProtocolVersion` | `object` | `commonMain` | `core/protocol/src/commonMain/kotlin/com/cbgm/sparrow/core/protocol/version/ProtocolVersion.kt` |
