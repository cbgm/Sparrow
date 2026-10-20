package com.cbgm.sparrow.core.base.message.domain.model

/**
 * Persisted message transport-mode markers that are not transport-encryption modes.
 * Writers (conversation orchestration) and readers (Chats) share these values.
 */
object MessageTransportMode {
    const val GROUP_END_TO_END_ENCRYPTED = "GROUP_E2EE"
    const val GROUP_MEMBER_ADDED = "SYSTEM_GROUP_MEMBER_ADDED"
    const val GROUP_MEMBER_REMOVED = "SYSTEM_GROUP_MEMBER_REMOVED"
    const val GROUP_MEMBER_LEFT = "SYSTEM_GROUP_MEMBER_LEFT"
    const val LOCAL_GROUP_MEMBERSHIP_STARTED = "SYSTEM_LOCAL_GROUP_MEMBERSHIP_STARTED"
    const val LOCAL_GROUP_MEMBERSHIP_REMOVED = "SYSTEM_LOCAL_GROUP_MEMBERSHIP_REMOVED"
    const val LOCAL_GROUP_MEMBERSHIP_LEFT = "SYSTEM_LOCAL_GROUP_MEMBERSHIP_LEFT"
    const val LOCAL_CONVERSATION_DELETED = "SYSTEM_LOCAL_CONVERSATION_DELETED"
}
