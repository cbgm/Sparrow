package com.cbgm.securechat.feature.chats.presentation.details.mapper

import com.cbgm.securechat.feature.chats.domain.model.group.GroupVerificationMembershipStatus
import com.cbgm.securechat.feature.chats.domain.model.group.GroupVerificationPair
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberVerificationState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupVerificationUiMapperTest {
    @Test
    fun adminCanChooseAnyActiveUnverifiedParticipant() {
        val summary =
            buildGroupVerificationSummary(
                isLocalAdmin = true,
                ownerContactId = null,
                ownerDisplayName = "Admin",
                ownInvitationId = null,
                rows =
                    listOf(
                        pair(
                            invitationId = "a",
                            contactId = "contact-a",
                            displayName = "Anna",
                            active = true,
                            adminVerified = false,
                            participantVerified = false
                        ),
                        pair(
                            invitationId = "b",
                            contactId = "contact-b",
                            displayName = "Bob",
                            active = true,
                            adminVerified = false,
                            participantVerified = true
                        ),
                        pair(
                            invitationId = "c",
                            contactId = "contact-c",
                            displayName = "Charlie",
                            active = false,
                            adminVerified = false,
                            participantVerified = false
                        )
                    ),
                currentMemberContactIds = setOf("contact-a", "contact-b")
            )

        assertEquals(4, summary.totalMemberCount)
        assertEquals(2, summary.activeParticipantCount)
        assertEquals(0, summary.mutuallyVerifiedParticipantCount)
        assertTrue(summary.members.single { it.displayName == "Anna" }.canVerify)
        assertTrue(summary.members.single { it.displayName == "Bob" }.canVerify)
        assertFalse(summary.members.single { it.displayName == "Charlie" }.canVerify)
    }

    @Test
    fun participantCanVerifyOnlyAdminAndSeesSameCounts() {
        val rows =
            listOf(
                pair(
                    invitationId = "self",
                    contactId = null,
                    displayName = "Participant",
                    active = true,
                    adminVerified = true,
                    participantVerified = false
                ),
                pair(
                    invitationId = "other",
                    contactId = null,
                    displayName = "Other participant",
                    active = true,
                    adminVerified = true,
                    participantVerified = true
                )
            )
        val summary =
            buildGroupVerificationSummary(
                isLocalAdmin = false,
                ownerContactId = "admin-contact",
                ownerDisplayName = "Admin",
                ownInvitationId = "self",
                rows = rows,
                remoteAdminContactIds = setOf("admin-contact"),
                currentMemberContactIds = setOf("admin-contact")
            )

        assertEquals(3, summary.totalMemberCount)
        assertEquals(2, summary.activeParticipantCount)
        assertEquals(1, summary.mutuallyVerifiedParticipantCount)
        assertTrue(summary.members.single { it.isGroupAdmin }.canVerify)
        assertEquals(
            GroupMemberVerificationState.MUTUALLY_VERIFIED,
            summary.members.single { it.displayName == "Other participant" }.state
        )
        assertFalse(summary.members.single { it.displayName == "Other participant" }.canVerify)
    }

    @Test
    fun participantAdminAndOwnRowsHaveDifferentLazyListKeys() {
        val summary =
            buildGroupVerificationSummary(
                isLocalAdmin = false,
                ownerContactId = "admin-contact",
                ownerDisplayName = "Admin",
                ownInvitationId = "same-invitation",
                rows =
                    listOf(
                        pair(
                            invitationId = "same-invitation",
                            contactId = null,
                            displayName = "Participant",
                            active = true,
                            adminVerified = false,
                            participantVerified = false
                        )
                    ),
                remoteAdminContactIds = setOf("admin-contact"),
                currentMemberContactIds = setOf("admin-contact")
            )

        assertEquals(
            expected = summary.members.size,
            actual =
                summary.members
                    .map(GroupMemberVerificationUiState::stableKey)
                    .distinct()
                    .size
        )
    }

    @Test
    fun formerReferenceAdminIsNotKeptAsGhostMember() {
        val summary =
            buildGroupVerificationSummary(
                isLocalAdmin = false,
                ownerContactId = "former-admin",
                ownerDisplayName = "Former admin",
                ownInvitationId = "self",
                rows =
                    listOf(
                        pair(
                            invitationId = "self",
                            contactId = null,
                            displayName = "Participant",
                            active = true,
                            adminVerified = true,
                            participantVerified = true
                        )
                    ),
                currentMemberContactIds = emptySet(),
                remoteAdminContactIds = emptySet(),
                isOrphaned = true
            )

        assertEquals(listOf("Participant"), summary.members.map { it.displayName })
        assertEquals(1, summary.totalMemberCount)
        assertTrue(summary.isOrphaned)
    }

    @Test
    fun activeNormalMemberCanLeaveWithoutOwnVerificationRowLookup() {
        val summary =
            buildGroupVerificationSummary(
                isLocalAdmin = false,
                isLocalMemberActive = true,
                ownerContactId = "admin-contact",
                ownerDisplayName = "Admin",
                ownInvitationId = null,
                rows = emptyList(),
                remoteAdminContactIds = setOf("admin-contact"),
                currentMemberContactIds = setOf("admin-contact")
            )

        assertTrue(summary.canLeaveGroup)
    }

    @Test
    fun retiredMemberCannotLeaveAgain() {
        val summary =
            buildGroupVerificationSummary(
                isLocalAdmin = false,
                isLocalMemberActive = false,
                ownerContactId = null,
                ownerDisplayName = "",
                ownInvitationId = null,
                rows = emptyList()
            )

        assertFalse(summary.canLeaveGroup)
    }

    @Test
    fun soleLocalAdminHasAuthoritativeStateWithoutRemoteMembers() {
        val summary =
            buildGroupVerificationSummary(
                isLocalAdmin = true,
                ownerContactId = null,
                ownerDisplayName = "Admin",
                ownInvitationId = null,
                rows = emptyList()
            )

        assertTrue(summary.hasAuthoritativeState)
        assertEquals(1, summary.totalMemberCount)
    }

    @Test
    fun staleActiveVerificationRowIsRemovedWhenMemberLeavesCurrentEpoch() {
        val summary =
            buildGroupVerificationSummary(
                isLocalAdmin = true,
                ownerContactId = null,
                ownerDisplayName = "Admin",
                ownInvitationId = null,
                rows =
                    listOf(
                        pair(
                            invitationId = "active",
                            contactId = "contact-active",
                            displayName = "Active",
                            active = true,
                            adminVerified = false,
                            participantVerified = false
                        ),
                        pair(
                            invitationId = "left",
                            contactId = "contact-left",
                            displayName = "Left",
                            active = true,
                            adminVerified = true,
                            participantVerified = true
                        )
                    ),
                currentMemberContactIds = setOf("contact-active")
            )

        assertEquals(2, summary.totalMemberCount)
        assertEquals(listOf("Admin", "Active"), summary.members.map { it.displayName })
    }

    @Test
    fun groupScopedRowsDoNotReuseAnotherGroupVerification() {
        val firstGroup =
            pair(
                groupId = "group-one",
                invitationId = "invite",
                contactId = "contact",
                displayName = "Anna",
                active = true,
                adminVerified = true,
                participantVerified = true
            )
        val secondGroup =
            pair(
                groupId = "group-two",
                invitationId = "invite",
                contactId = "contact",
                displayName = "Anna",
                active = true,
                adminVerified = false,
                participantVerified = false
            )

        assertTrue(firstGroup.adminVerifiedParticipant)
        assertTrue(firstGroup.participantVerifiedAdmin)
        assertFalse(secondGroup.adminVerifiedParticipant)
        assertFalse(secondGroup.participantVerifiedAdmin)
    }

    private fun pair(
        groupId: String = "group",
        invitationId: String,
        contactId: String?,
        displayName: String,
        active: Boolean,
        adminVerified: Boolean,
        participantVerified: Boolean
    ): GroupVerificationPair =
        GroupVerificationPair(
            groupId = groupId,
            invitationId = invitationId,
            contactId = contactId,
            displayName = displayName,
            membershipStatus =
                if (active) {
                    GroupVerificationMembershipStatus.ACTIVE
                } else {
                    GroupVerificationMembershipStatus.PENDING
                },
            adminVerifiedParticipant = adminVerified,
            participantVerifiedAdmin = participantVerified,
            updatedAtEpochMilliseconds = 1L
        )
}
