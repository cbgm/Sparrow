package com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.data.database.entity.GroupMetadataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A rotation welcome carries the title without a version, so it must not roll back a title an admin changed;
 * versioned saves only replace the version they checked.
 */
class GroupMetadataVersionTest {
    private val dao = FakeGroupMetadataDao()

    @Test
    fun welcomeTitleIsSetOnlyWhileNoAdminVersionIsStored() = runBlocking {
        dao.upsertTitleValueIfUnversioned(GROUP_ID, "Welcome title")
        assertEquals("Welcome title", dao.rows.getValue(GROUP_ID).title)

        dao.upsert(dao.rows.getValue(GROUP_ID).copy(title = "Renamed", titleChangedAtEpochMilliseconds = 9L))
        dao.upsertTitleValueIfUnversioned(GROUP_ID, "Stale welcome title")

        assertEquals("Renamed", dao.rows.getValue(GROUP_ID).title)
        assertEquals(9L, dao.rows.getValue(GROUP_ID).titleChangedAtEpochMilliseconds)
    }

    @Test
    fun versionedTitleSaveReplacesOnlyTheCheckedVersion() = runBlocking {
        dao.upsert(GroupMetadataEntity(groupId = GROUP_ID, title = "Old", titleChangedAtEpochMilliseconds = 5L))

        assertFalse(dao.replaceTitleIfVersion(GROUP_ID, 4L, "Lost race", 6L, publishPending = true))
        assertTrue(dao.replaceTitleIfVersion(GROUP_ID, 5L, "New", 6L, publishPending = true))

        val row = dao.rows.getValue(GROUP_ID)
        assertEquals("New", row.title)
        assertEquals(6L, row.titleChangedAtEpochMilliseconds)
        assertTrue(row.titlePublishPending)
    }

    private class FakeGroupMetadataDao : GroupMetadataDao {
        val rows = mutableMapOf<String, GroupMetadataEntity>()

        override fun observe(groupId: String): Flow<GroupMetadataEntity?> = error(NOT_USED)

        override suspend fun get(groupId: String): GroupMetadataEntity? = rows[groupId]

        override suspend fun upsert(entity: GroupMetadataEntity) {
            rows[entity.groupId] = entity
        }

        override suspend fun markTitlePublished(groupId: String, changedAtEpochMilliseconds: Long) = error(NOT_USED)

        override suspend fun markDescriptionPublished(groupId: String, changedAtEpochMilliseconds: Long) =
            error(NOT_USED)

        override suspend fun markAvatarPublished(groupId: String, changedAtEpochMilliseconds: Long) = error(NOT_USED)

        override suspend fun findTitlePublishPendingGroupIds(): List<String> = error(NOT_USED)

        override suspend fun findDescriptionPublishPendingGroupIds(): List<String> = error(NOT_USED)

        override suspend fun findAvatarPublishPendingGroupIds(): List<String> = error(NOT_USED)

        override suspend fun clearTitleChangedAt(groupId: String) = error(NOT_USED)

        override suspend fun clearDescription(groupId: String) = error(NOT_USED)

        override suspend fun clearAvatar(groupId: String) = error(NOT_USED)
    }

    private companion object {
        const val NOT_USED = "Not used by the metadata version tests"
        const val GROUP_ID = "group-1"
    }
}
