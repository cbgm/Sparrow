package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

/** The local member's part in group verification, from the group's security state. */
sealed interface GroupVerificationRole {
    /** The local member is the group admin and owns the verification rows. */
    data object Admin : GroupVerificationRole

    /** No security state exists, e.g. the owner before the first member joined or after it was removed. */
    data object WithoutSecurity : GroupVerificationRole

    /**
     * The local member is an ordinary member; [adminContactId] is the admin who sent the last welcome, when known,
     * and [currentAdminContactIds] are all admins of the current epoch, including promoted ones.
     */
    data class Member(
        val adminContactId: String?,
        val currentAdminContactIds: Set<String> = emptySet()
    ) : GroupVerificationRole {
        /**
         * A member may verify only an admin: any admin of the current epoch, as the receipt
         * (requireCurrentRemoteAdmin) and the verification snapshot accept.
         */
        fun isAdmin(contactId: String): Boolean = contactId in currentAdminContactIds
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
