package com.cbgm.securechat.data.database.di

import com.cbgm.securechat.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.data.database.SecureChatDatabase
import com.cbgm.securechat.data.database.factory.buildSecureChatDatabase
import com.cbgm.securechat.data.database.factory.createAndroidDatabaseBuilder
import com.cbgm.securechat.data.database.identity.LocalIdentityDataResetter
import com.cbgm.securechat.data.database.identity.RoomLocalIdentityDataResetter
import com.cbgm.securechat.data.database.mailbox.RoomMailboxRouteRepository
import com.cbgm.securechat.data.database.outbox.DefaultProtocolOutbox
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android database dependency graph.
 */
val androidDatabaseModule =
    module {

        single<SecureChatDatabase> {
            buildSecureChatDatabase(builder = createAndroidDatabaseBuilder(context = androidContext()))
        }

        single<LocalIdentityDataResetter> {
            RoomLocalIdentityDataResetter(database = get())
        }

        single {
            get<SecureChatDatabase>().contactDao()
        }

        single {
            get<SecureChatDatabase>().groupVerificationDao()
        }

        single {
            get<SecureChatDatabase>().chatDao()
        }

        single {
            get<SecureChatDatabase>().groupSecurityDao()
        }

        single {
            get<SecureChatDatabase>().groupInvitationDao()
        }

        single {
            get<SecureChatDatabase>().identityInvitationDao()
        }

        single {
            get<SecureChatDatabase>().contactRoutingIdDao()
        }

        single {
            get<SecureChatDatabase>().protocolOutboxDao()
        }

        single {
            get<SecureChatDatabase>().messageDeliveryStatusDao()
        }

        single {
            get<SecureChatDatabase>().messageRecipientStateDao()
        }

        single {
            get<SecureChatDatabase>().mailboxRouteDao()
        }

        single<MailboxRouteRepository> {
            RoomMailboxRouteRepository(dao = get())
        }

        single<ProtocolOutbox> {
            DefaultProtocolOutbox(
                outboxDao = get(),
                packetCodec = get()
            )
        }
    }
