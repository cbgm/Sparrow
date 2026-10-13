package com.cbgm.sparrow.data.datastore.group

/** Preference keys of group metadata. Chats observes them; conversation orchestration writes them. */
object GroupMetadataPreferenceKeys {
    fun avatarChangedAt(groupId: String): String = "chats.group_avatar.changed_at.$groupId"

    fun titleChangedAt(groupId: String): String = "chats.group_title.changed_at.$groupId"

    fun descriptionValue(groupId: String): String = "chats.group_description.value.$groupId"

    fun descriptionChangedAt(groupId: String): String = "chats.group_description.changed_at.$groupId"
}
