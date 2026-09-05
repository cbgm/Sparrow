package com.cbgm.sparrow.feature.chats.device

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.data.datastore.createSparrowDataStore
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

            dataSource.save(groupId, 1, firstKey)
            dataSource.save(groupId, 2, secondKey)

            assertContentEquals(firstKey, dataSource.load(groupId, 1))
            assertContentEquals(secondKey, dataSource.load(groupId, 2))

            dataSource.deleteBefore(groupId, 2)

            assertNull(dataSource.load(groupId, 1))
            assertContentEquals(secondKey, dataSource.load(groupId, 2))

            dataSource.deleteGroup(groupId)

            assertNull(dataSource.load(groupId, 2))
        }
}
