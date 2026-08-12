package com.cbgm.securechat.data.database.factory

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cbgm.securechat.data.database.SecureChatDatabase
import com.cbgm.securechat.data.database.migration.DatabaseMigrations
import kotlinx.coroutines.Dispatchers

/**
 * Applies database configuration shared by all platforms.
 */
fun buildSecureChatDatabase(builder: RoomDatabase.Builder<SecureChatDatabase>): SecureChatDatabase =
    builder
        .addMigrations(
            DatabaseMigrations.Migration9To10,
            DatabaseMigrations.Migration10To11,
            DatabaseMigrations.Migration11To12,
            DatabaseMigrations.Migration12To13,
            DatabaseMigrations.Migration13To14,
            DatabaseMigrations.Migration14To15,
            DatabaseMigrations.Migration15To16,
            DatabaseMigrations.Migration16To17,
            DatabaseMigrations.Migration17To18,
            DatabaseMigrations.Migration18To19,
            DatabaseMigrations.Migration19To20,
            DatabaseMigrations.Migration20To21,
            DatabaseMigrations.Migration21To22,
            DatabaseMigrations.Migration22To23,
            DatabaseMigrations.Migration23To24,
            DatabaseMigrations.Migration24To25
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
