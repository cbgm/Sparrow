package com.cbgm.sparrow.data.database.device

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cbgm.sparrow.data.database.SparrowDatabase
import com.cbgm.sparrow.data.database.migration.MessagePartPayloadMigration53To54
import kotlinx.coroutines.Dispatchers

/**
 * Applies database configuration shared by all platforms.
 */
fun buildSparrowDatabase(builder: RoomDatabase.Builder<SparrowDatabase>): SparrowDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MessagePartPayloadMigration53To54)
        .build()
