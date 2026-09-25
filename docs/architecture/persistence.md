# Persistence model

The client database is Room/SQLite in `:data:database`. The current `SparrowDatabase` schema version is **53**.

## Current entities

`SparrowDatabase` currently registers these entities:

- `AutoReplyEntity`, `AutoReplyRecipientEntity`
- `ContactEntity`, `ContactPhoneNumberEntity`, `ContactPublicIdentityEntity`, `ContactRoutingIdEntity`
- `PendingRemoteIdentityChangeEntity`, `ApprovedIdentityReconnectionEntity`, `IdentityExchangeEntity`
- `ConversationEntity`, `ConversationParticipantEntity`
- `GroupSecurityStateEntity`, `GroupMemberKeyEntity`, `GroupMembershipEntity`, `GroupPinEntity`, `GroupVerificationPairEntity`
- `InvitationEntity`
- `MessageEntity`, `MessageAttachmentEntity`, `AttachmentMessageContextEntity`, `MessageRecipientStateEntity`, `MessageReactionEntity`
- `MessageSearchEmbeddingEntity`, `MessageSafetyAssessmentEntity`
- `ProtocolOutboxEntity`, `ProtocolOutboxFailureEventEntity`
- `LocalMailboxCredentialEntity`, `RemoteMailboxRouteEntity`
- `LinkPreviewEntity`

## Recent explicit migrations

`DatabaseBuilder` installs explicit migrations for the later schema changes:

- `IdentityExchangeMigration43To44`
- `InvitationPeerDetailsMigration44To45`
- `AttachmentMessageContextMigration45To46`
- `AttachmentMessageContextMigration46To47`
- `GroupMemberPhoneMigration47To48`
- `ProtocolOutboxFailuresMigration48To49`
- `PendingRemoteIdentityChangeMigration49To50`
- `PendingRemoteIdentityChangeMigration50To51`
- `ApprovedIdentityReconnectionMigration51To52`
- `ApprovedIdentityOfferMigration52To53`

This migration history is useful when diagnosing behavior introduced by recovery/invite/attachment refactors: those features have durable schema state and cannot be treated as only UI changes.

## Ownership rule

Room DAOs/entities live in `:data:database`, but feature repository contracts remain in their owning feature modules. A feature datasource may use its DAO; a repository implementation may use its own datasources. Repositories must not call unrelated repositories to create cross-feature workflows.
