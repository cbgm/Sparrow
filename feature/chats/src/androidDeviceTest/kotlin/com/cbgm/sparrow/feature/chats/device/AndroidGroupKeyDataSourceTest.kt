package com.cbgm.sparrow.feature.chats.device

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.core.datastore.createSparrowDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class AndroidGroupKeyDataSourceTest {
    @Test
    fun storesGroupKeysEncryptedByGroupAndEpoch() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val dataStore =
                createSparrowDataStore(
                    filePath = context.filesDir.resolve("group-key-test-${System.nanoTime()}.preferences_pb").absolutePath
                )
            val dataSource = AndroidGroupKeyDataSource(dataStore = dataStore)
            val groupId = "storage-test-${System.currentTimeMillis()}"
            val firstKey = ByteArray(32) { 1 }
            val secondKey = ByteArray(32) { 2 }

            dataSource.save(groupId, 1, firstKey).getOrThrow()
            dataSource.save(groupId, 2, secondKey).getOrThrow()

            assertContentEquals(firstKey, dataSource.load(groupId, 1).getOrThrow())
            assertContentEquals(secondKey, dataSource.load(groupId, 2).getOrThrow())

            dataSource.deleteBefore(groupId, 2).getOrThrow()

            assertNull(dataSource.load(groupId, 1).getOrThrow())
            assertContentEquals(secondKey, dataSource.load(groupId, 2).getOrThrow())

            dataSource.deleteGroup(groupId).getOrThrow()

            assertNull(dataSource.load(groupId, 2).getOrThrow())
        }
}
