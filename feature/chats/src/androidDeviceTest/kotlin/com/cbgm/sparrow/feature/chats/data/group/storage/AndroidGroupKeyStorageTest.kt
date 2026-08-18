package com.cbgm.sparrow.feature.chats.data.group.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.core.datastore.createSparrowDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class AndroidGroupKeyStorageTest {
    @Test
    fun storesGroupKeysEncryptedByGroupAndEpoch() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val dataStore =
                createSparrowDataStore(
                    filePath = context.filesDir.resolve("group-key-test-${System.nanoTime()}.preferences_pb").absolutePath
                )
            val storage = AndroidGroupKeyStorage(dataStore = dataStore)
            val groupId = "storage-test-${System.currentTimeMillis()}"
            val firstKey = ByteArray(32) { 1 }
            val secondKey = ByteArray(32) { 2 }

            storage.save(groupId, 1, firstKey).getOrThrow()
            storage.save(groupId, 2, secondKey).getOrThrow()

            assertContentEquals(firstKey, storage.load(groupId, 1).getOrThrow())
            assertContentEquals(secondKey, storage.load(groupId, 2).getOrThrow())

            storage.deleteBefore(groupId, 2).getOrThrow()

            assertNull(storage.load(groupId, 1).getOrThrow())
            assertContentEquals(secondKey, storage.load(groupId, 2).getOrThrow())

            storage.deleteGroup(groupId).getOrThrow()

            assertNull(storage.load(groupId, 2).getOrThrow())
        }
}
