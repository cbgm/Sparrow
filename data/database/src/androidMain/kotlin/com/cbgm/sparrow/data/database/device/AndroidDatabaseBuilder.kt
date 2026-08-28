package com.cbgm.sparrow.data.database.factory

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cbgm.sparrow.data.database.SparrowDatabase
import com.cbgm.sparrow.data.database.util.DatabaseConstants

/**
 * Creates the Android Room builder for the Sparrow database.
 *
 * The application context is used so the database does not retain
 * an Activity or other short-lived Android component.
 */
fun createAndroidDatabaseBuilder(
    context: Context
): RoomDatabase.Builder<SparrowDatabase> {
    val databaseFile = context.getDatabasePath(DatabaseConstants.DATABASE_NAME)

    return Room.databaseBuilder<SparrowDatabase>(
        context = context.applicationContext,
        name = databaseFile.absolutePath
    )
}
