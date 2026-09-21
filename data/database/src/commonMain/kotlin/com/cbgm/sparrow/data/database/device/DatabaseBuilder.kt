package com.cbgm.sparrow.data.database.factory

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cbgm.sparrow.data.database.SparrowDatabase
import com.cbgm.sparrow.data.database.migration.AttachmentMessageContextMigration45To46
import com.cbgm.sparrow.data.database.migration.AttachmentMessageContextMigration46To47
import com.cbgm.sparrow.data.database.migration.GroupMemberPhoneMigration47To48
import com.cbgm.sparrow.data.database.migration.IdentityExchangeMigration43To44
import com.cbgm.sparrow.data.database.migration.InvitationPeerDetailsMigration44To45
import com.cbgm.sparrow.data.database.migration.ProtocolOutboxFailuresMigration48To49
import kotlinx.coroutines.Dispatchers

/**
 * Applies database configuration shared by all platforms.
 */
fun buildSparrowDatabase(builder: RoomDatabase.Builder<SparrowDatabase>): SparrowDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(
            IdentityExchangeMigration43To44,
            InvitationPeerDetailsMigration44To45,
            AttachmentMessageContextMigration45To46,
            AttachmentMessageContextMigration46To47,
            GroupMemberPhoneMigration47To48,
            ProtocolOutboxFailuresMigration48To49
        )
        .build()
