package com.cbgm.sparrow.feature.chats.data.group.security

import com.cbgm.sparrow.feature.membership.data.model.GROUP_ADMIN_ROLE as MEMBERSHIP_GROUP_ADMIN_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE as MEMBERSHIP_GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GROUP_MEMBER_ROLE as MEMBERSHIP_GROUP_MEMBER_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GROUP_OWNER_ROLE as MEMBERSHIP_GROUP_OWNER_ROLE
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole as isMembershipGroupAdminRole

const val GROUP_OWNER_ROLE = MEMBERSHIP_GROUP_OWNER_ROLE
const val GROUP_ADMIN_ROLE = MEMBERSHIP_GROUP_ADMIN_ROLE
const val GROUP_MEMBER_ROLE = MEMBERSHIP_GROUP_MEMBER_ROLE
const val GROUP_LEFT_ROLE = MEMBERSHIP_GROUP_LEFT_ROLE

fun String.isGroupAdminRole(): Boolean = isMembershipGroupAdminRole()
