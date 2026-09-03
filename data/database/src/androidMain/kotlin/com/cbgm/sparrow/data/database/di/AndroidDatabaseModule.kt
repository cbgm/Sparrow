package com.cbgm.sparrow.data.database.di

import com.cbgm.sparrow.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.data.database.SparrowDatabase
import com.cbgm.sparrow.data.database.factory.buildSparrowDatabase
import com.cbgm.sparrow.data.database.factory.createAndroidDatabaseBuilder
import com.cbgm.sparrow.data.database.identity.LocalIdentityDataResetter
import com.cbgm.sparrow.data.database.identity.RoomLocalIdentityDataResetter
import com.cbgm.sparrow.data.database.mailbox.RoomMailboxRouteRepository
import com.cbgm.sparrow.data.database.outbox.DefaultProtocolOutbox
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android database dependency graph.
 */
val androidDatabaseModule =
    module {

        single<SparrowDatabase> {
            buildSparrowDatabase(builder = createAndroidDatabaseBuilder(context = androidContext()))
        }

        single<LocalIdentityDataResetter> {
            RoomLocalIdentityDataResetter(database = get())
        }

        single {
            get<SparrowDatabase>().contactDao()
        }

        single {
            get<SparrowDatabase>().groupVerificationDao()
        }

        single {
            get<SparrowDatabase>().chatDao()
        }

        single {
            get<SparrowDatabase>().groupSecurityDao()
        }

        single {
            get<SparrowDatabase>().groupInvitationDao()
        }

        single {
            get<SparrowDatabase>().identityInvitationDao()
        }

        single {
            get<SparrowDatabase>().contactRoutingIdDao()
        }

        single {
            get<SparrowDatabase>().protocolOutboxDao()
        }

        single {
            get<SparrowDatabase>().messageDeliveryStatusDao()
        }

        single {
            get<SparrowDatabase>().messageAttachmentDao()
        }

        single {
            get<SparrowDatabase>().messageRecipientStateDao()
        }

        single {
            get<SparrowDatabase>().messageReactionDao()
        }

        single {
            get<SparrowDatabase>().messageSearchDao()
        }

        single {
            get<SparrowDatabase>().messageSafetyDao()
        }

        single {
            get<SparrowDatabase>().mailboxRouteDao()
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
