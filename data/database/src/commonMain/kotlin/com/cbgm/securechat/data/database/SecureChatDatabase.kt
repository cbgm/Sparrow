package com.cbgm.securechat.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.ContactRoutingIdDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.data.database.dao.IdentityInvitationDao
import com.cbgm.securechat.data.database.dao.MailboxRouteDao
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.data.database.dao.MessageRecipientStateDao
import com.cbgm.securechat.data.database.dao.ProtocolOutboxDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.securechat.data.database.entity.ContactRoutingIdEntity
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.data.database.entity.GroupSecurityStateEntity
import com.cbgm.securechat.data.database.entity.GroupVerificationPairEntity
import com.cbgm.securechat.data.database.entity.IdentityInvitationEntity
import com.cbgm.securechat.data.database.entity.LocalMailboxCredentialEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.data.database.entity.ProtocolOutboxEntity
import com.cbgm.securechat.data.database.entity.RemoteMailboxRouteEntity

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
        MessageRecipientStateEntity::class,
        ProtocolOutboxEntity::class,
        LocalMailboxCredentialEntity::class,
        RemoteMailboxRouteEntity::class
    ],
    version = 25,
    exportSchema = true
)
@ConstructedBy(SecureChatDatabaseConstructor::class)
abstract class SecureChatDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    abstract fun chatDao(): ChatDao

    abstract fun groupSecurityDao(): GroupSecurityDao

    abstract fun groupInvitationDao(): GroupInvitationDao

    abstract fun groupVerificationDao(): GroupVerificationDao

    abstract fun identityInvitationDao(): IdentityInvitationDao

    abstract fun contactRoutingIdDao(): ContactRoutingIdDao

    abstract fun protocolOutboxDao(): ProtocolOutboxDao

    abstract fun messageDeliveryStatusDao(): MessageDeliveryStatusDao

    abstract fun messageRecipientStateDao(): MessageRecipientStateDao

    abstract fun mailboxRouteDao(): MailboxRouteDao
}
