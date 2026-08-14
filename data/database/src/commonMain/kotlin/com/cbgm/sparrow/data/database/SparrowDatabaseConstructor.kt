package com.cbgm.sparrow.data.database

import androidx.room.RoomDatabaseConstructor

/**
 * Room generates the platform implementation of this object.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SparrowDatabaseConstructor : RoomDatabaseConstructor<SparrowDatabase> {
    override fun initialize(): SparrowDatabase
}
