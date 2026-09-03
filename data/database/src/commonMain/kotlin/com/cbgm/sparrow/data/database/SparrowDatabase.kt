package com.cbgm.sparrow.data.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.dao.IdentityInvitationDao
import com.cbgm.sparrow.data.database.dao.MailboxRouteDao
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.dao.MessageSafetyDao
import com.cbgm.sparrow.data.database.dao.MessageSearchDao
import com.cbgm.sparrow.data.database.dao.ProtocolOutboxDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupSecurityStateEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.data.database.entity.IdentityInvitationEntity
import com.cbgm.sparrow.data.database.entity.LocalMailboxCredentialEntity
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.data.database.entity.MessageSafetyAssessmentEntity
import com.cbgm.sparrow.data.database.entity.MessageSearchEmbeddingEntity
import com.cbgm.sparrow.data.database.entity.ProtocolOutboxEntity
import com.cbgm.sparrow.data.database.entity.RemoteMailboxRouteEntity

@Database(
    entities = [
        ContactEntity::class,
        ContactPhoneNumberEntity::class,
        ContactPublicIdentityEntity::class,
        ContactRoutingIdEntity::class,
        ConversationEntity::class,
        ConversationParticipantEntity::class,
        GroupSecurityStateEntity::class,
        GroupMemberKeyEntity::class,
        GroupInvitationEntity::class,
        GroupVerificationPairEntity::class,
        IdentityInvitationEntity::class,
        MessageEntity::class,
        MessageAttachmentEntity::class,
        MessageSearchEmbeddingEntity::class,
        MessageSafetyAssessmentEntity::class,
        MessageRecipientStateEntity::class,
        MessageReactionEntity::class,
        ProtocolOutboxEntity::class,
        LocalMailboxCredentialEntity::class,
        RemoteMailboxRouteEntity::class
    ],
    version = 33,
    autoMigrations = [
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 28, to = 29),
        AutoMigration(from = 29, to = 30),
        AutoMigration(from = 30, to = 31),
        AutoMigration(from = 31, to = 32),
        AutoMigration(from = 32, to = 33)
    ],
    exportSchema = true
)
@ConstructedBy(SparrowDatabaseConstructor::class)
abstract class SparrowDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    abstract fun chatDao(): ChatDao

    abstract fun groupSecurityDao(): GroupSecurityDao

    abstract fun groupInvitationDao(): GroupInvitationDao

    abstract fun groupVerificationDao(): GroupVerificationDao

    abstract fun identityInvitationDao(): IdentityInvitationDao

    abstract fun contactRoutingIdDao(): ContactRoutingIdDao

    abstract fun protocolOutboxDao(): ProtocolOutboxDao

    abstract fun messageDeliveryStatusDao(): MessageDeliveryStatusDao

    abstract fun messageAttachmentDao(): MessageAttachmentDao

    abstract fun messageRecipientStateDao(): MessageRecipientStateDao

    abstract fun messageReactionDao(): MessageReactionDao

    abstract fun messageSearchDao(): MessageSearchDao

    abstract fun messageSafetyDao(): MessageSafetyDao

    abstract fun mailboxRouteDao(): MailboxRouteDao
}
