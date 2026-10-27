package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

/** The local member's part in group verification, from the group's security state. */
sealed interface GroupVerificationRole {
    /** The local member is the group admin and owns the verification rows. */
    data object Admin : GroupVerificationRole

    /** No security state exists, e.g. the owner before the first member joined or after it was removed. */
    data object WithoutSecurity : GroupVerificationRole

    /** The local member is an ordinary member; [adminContactId] is the admin's contact when known. */
    data class Member(
        val adminContactId: String?
    ) : GroupVerificationRole {
        /** A member may verify only the admin. */
        fun isAdmin(contactId: String): Boolean = adminContactId == contactId
    }

    /** Only the admin, or the owner without security state, may publish verification state. */
    fun requireMayPublish() {
        check(this !is Member) { "Only a group admin may publish membership verification state" }
    }

    /** Verifying someone needs the group's security state. */
    fun requireSecurity() {
        check(this != WithoutSecurity) { "Group security state was not found" }
    }
}
