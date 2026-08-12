package com.cbgm.securechat.feature.chats.domain.model

data class GroupAdministrationState(
    val isLocalAdmin: Boolean = false,
    val isOrphaned: Boolean = false,
    val adminContactIds: Set<String> = emptySet(),
    val currentMemberContactIds: Set<String> = emptySet(),
    val promotableContactIds: Set<String> = emptySet(),
    val requiresPromotionBeforeLeave: Boolean = false,
    val activeMemberCount: Int = 0
)
