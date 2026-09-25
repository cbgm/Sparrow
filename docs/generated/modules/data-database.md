# `:data:database`

Source directory: `data/database`

## Direct project dependencies

- `:core`
- `:core:protocol`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `ApprovedIdentityReconnectionDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ApprovedIdentityReconnectionDao.kt` |
| `AutoReplyDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/AutoReplyDao.kt` |
| `ChatDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ChatDao.kt` |
| `ContactDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ContactDao.kt` |
| `ContactRoutingIdDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ContactRoutingIdDao.kt` |
| `GroupMembershipDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupMembershipDao.kt` |
| `GroupPinDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupPinDao.kt` |
| `GroupSecurityDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupSecurityDao.kt` |
| `GroupVerificationDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/GroupVerificationDao.kt` |
| `IdentityExchangeDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/IdentityExchangeDao.kt` |
| `InvitationDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/InvitationDao.kt` |
| `LinkPreviewDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/LinkPreviewDao.kt` |
| `MailboxRouteDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MailboxRouteDao.kt` |
| `MessageAttachmentDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageAttachmentDao.kt` |
| `MessageDeliveryStatusDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageDeliveryStatusDao.kt` |
| `MessageReactionDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageReactionDao.kt` |
| `MessageRecipientStateDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageRecipientStateDao.kt` |
| `MessageSafetyDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageSafetyDao.kt` |
| `MessageSearchDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/MessageSearchDao.kt` |
| `PendingRemoteIdentityChangeDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/PendingRemoteIdentityChangeDao.kt` |
| `ProtocolOutboxDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/ProtocolOutboxDao.kt` |
| `RemoteIdentityDao` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/dao/RemoteIdentityDao.kt` |
| `ApprovedIdentityReconnectionEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ApprovedIdentityReconnectionEntity.kt` |
| `AttachmentMessageContextEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AttachmentMessageContextEntity.kt` |
| `AutoReplyEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AutoReplyEntity.kt` |
| `AutoReplyRecipientEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/AutoReplyRecipientEntity.kt` |
| `ContactEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactEntity.kt` |
| `ContactPhoneNumberEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactPhoneNumberEntity.kt` |
| `ContactPublicIdentityEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactPublicIdentityEntity.kt` |
| `ContactRoutingIdEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ContactRoutingIdEntity.kt` |
| `ConversationEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationEntity.kt` |
| `ConversationParticipantEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationParticipantEntity.kt` |
| `ConversationParticipantRole` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationParticipantRole.kt` |
| `ConversationType` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ConversationType.kt` |
| `GroupMemberKeyEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupMemberKeyEntity.kt` |
| `GroupMembershipEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupMembershipEntity.kt` |
| `GroupPinEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupPinEntity.kt` |
| `GroupSecurityStateEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupSecurityStateEntity.kt` |
| `GroupVerificationPairEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/GroupVerificationPairEntity.kt` |
| `IdentityExchangeEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/IdentityExchangeEntity.kt` |
| `InvitationEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/InvitationEntity.kt` |
| `LinkPreviewEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/LinkPreviewEntity.kt` |
| `LocalMailboxCredentialEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MailboxRouteEntities.kt` |
| `MessageAttachmentEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageAttachmentEntity.kt` |
| `MessageEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageEntity.kt` |
| `MessageReactionEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageReactionEntity.kt` |
| `MessageRecipientStateEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageRecipientStateEntity.kt` |
| `MessageSafetyAssessmentEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageSafetyAssessmentEntity.kt` |
| `MessageSearchEmbeddingEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MessageSearchEmbeddingEntity.kt` |
| `PendingRemoteIdentityChangeEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/PendingRemoteIdentityChangeEntity.kt` |
| `ProtocolOutboxEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ProtocolOutboxEntity.kt` |
| `ProtocolOutboxFailureEventEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/ProtocolOutboxFailureEventEntity.kt` |
| `RemoteMailboxRouteEntity` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/entity/MailboxRouteEntities.kt` |
| `LocalIdentityDataResetter` | `interface` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/identity/LocalIdentityDataResetter.kt` |
| `RoomLocalIdentityDataResetter` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/identity/LocalIdentityDataResetter.kt` |
| `RoomMailboxRouteRepository` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/mailbox/RoomMailboxRouteRepository.kt` |
| `ApprovedIdentityOfferMigration52To53` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ApprovedIdentityOfferMigration52To53.kt` |
| `ApprovedIdentityReconnectionMigration51To52` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ApprovedIdentityReconnectionMigration51To52.kt` |
| `AttachmentMessageContextMigration45To46` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/AttachmentMessageContextMigration45To46.kt` |
| `AttachmentMessageContextMigration46To47` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/AttachmentMessageContextMigration46To47.kt` |
| `GroupMemberPhoneMigration47To48` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/GroupMemberPhoneMigration47To48.kt` |
| `IdentityExchangeMigration41To42` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/IdentityExchangeMigration41To42.kt` |
| `IdentityExchangeMigration43To44` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/IdentityExchangeMigration43To44.kt` |
| `InvitationPeerDetailsMigration44To45` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/InvitationPeerDetailsMigration44To45.kt` |
| `PendingRemoteIdentityChangeMigration49To50` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/PendingRemoteIdentityChangeMigration49To50.kt` |
| `PendingRemoteIdentityChangeMigration50To51` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/PendingRemoteIdentityChangeMigration50To51.kt` |
| `ProtocolOutboxFailuresMigration48To49` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/migration/ProtocolOutboxFailuresMigration48To49.kt` |
| `ContactWithPhoneNumbersDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ContactWithPhoneNumbersDto.kt` |
| `ContactWithPublicIdentityDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ContactWithPublicIdentityDto.kt` |
| `ConversationSummaryDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ConversationSummaryDto.kt` |
| `ConversationWithMessagesDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/ConversationWithMessagesDto.kt` |
| `LocalMessageAttachmentRowDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/LocalMessageAttachmentRowDto.kt` |
| `MessageCursorDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageCursorDto.kt` |
| `MessageSafetySourceDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageSafetySourceDto.kt` |
| `MessageSearchSourceDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/MessageSearchSourceDto.kt` |
| `StoredMessageEmbeddingDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/StoredMessageEmbeddingDto.kt` |
| `StoredMessageSearchMatchDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/StoredMessageSearchMatchDto.kt` |
| `UnreadIncomingMessageDto` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/model/UnreadIncomingMessageDto.kt` |
| `DefaultProtocolOutbox` | `class` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/outbox/DefaultProtocolOutbox.kt` |
| `DatabaseConstants` | `object` | `commonMain` | `data/database/src/commonMain/kotlin/com/cbgm/sparrow/data/database/util/DatabaseConstants.kt` |
