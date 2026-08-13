package com.cbgm.securechat.feature.chats.data.group.security

const val GROUP_OWNER_ROLE = "OWNER"
const val GROUP_ADMIN_ROLE = "ADMIN"
const val GROUP_MEMBER_ROLE = "MEMBER"

fun String.isGroupAdminRole(): Boolean =
    this == GROUP_OWNER_ROLE || this == GROUP_ADMIN_ROLE
