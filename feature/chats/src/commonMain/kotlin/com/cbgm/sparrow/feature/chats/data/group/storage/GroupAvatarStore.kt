package com.cbgm.sparrow.feature.chats.data.group.storage

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GroupAvatarStore(
    private val dataStore: SparrowDataStore,
    private val fileStorage: GroupAvatarFileStorage,
    private val cryptoHash: CryptoHash
) {
    fun observe(groupId: String): Flow<GroupAvatar> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return dataStore.observeLong(changedAtKey(groupId)).map { changedAt ->
            GroupAvatar(
                groupId = groupId,
                changedAtEpochMilliseconds = changedAt,
                bytes = fileStorage.read(fileName(groupId))
            )
        }
    }

    suspend fun get(groupId: String): GroupAvatar {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return GroupAvatar(
            groupId = groupId,
            changedAtEpochMilliseconds = dataStore.getLong(changedAtKey(groupId)),
            bytes = fileStorage.read(fileName(groupId))
        )
    }

    suspend fun save(
        groupId: String,
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(bytes.isNotEmpty()) { "Group avatar must not be empty" }
        fileStorage.write(fileName(groupId), bytes)
        dataStore.edit { putLong(changedAtKey(groupId), changedAtEpochMilliseconds) }
    }

    suspend fun remove(
        groupId: String,
        changedAtEpochMilliseconds: Long
    ) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        fileStorage.delete(fileName(groupId))
        dataStore.edit { putLong(changedAtKey(groupId), changedAtEpochMilliseconds) }
    }

    suspend fun deleteLocal(groupId: String) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        fileStorage.delete(fileName(groupId))
        dataStore.edit { removeLong(changedAtKey(groupId)) }
    }

    private fun changedAtKey(groupId: String): String = "$CHANGED_AT_PREFIX$groupId"

    private fun fileName(groupId: String): String =
        cryptoHash
            .sha256(groupId.encodeToByteArray())
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(2, '0')
            } + ".jpg"

    private companion object {
        const val CHANGED_AT_PREFIX = "chats.group_avatar.changed_at."
    }
}
