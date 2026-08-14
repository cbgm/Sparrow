package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity

internal data class GroupLocalMembershipTimeline(
    val visibleMessages: List<MessageEntity>,
    val currentInvitations: List<GroupInvitationEntity>,
    val isLocallyInactive: Boolean
)

internal fun buildGroupLocalMembershipTimeline(
    messages: List<MessageEntity>,
    invitations: List<GroupInvitationEntity>
): GroupLocalMembershipTimeline {
    val orderedMessages =
        messages.sortedWith(
            compareBy<MessageEntity>(MessageEntity::createdAtEpochMilliseconds)
                .thenBy(MessageEntity::id)
        )
    val latestStartAt =
        orderedMessages
            .filter { message -> GroupMembershipMessageFactory.isLocalMembershipStart(message.transportMode) }
            .maxOfOrNull(MessageEntity::createdAtEpochMilliseconds)
    val latestEndAt =
        orderedMessages
            .filter { message -> GroupMembershipMessageFactory.isLocalMembershipEnd(message.transportMode) }
            .maxOfOrNull(MessageEntity::createdAtEpochMilliseconds)
    val locallyInactive =
        latestEndAt != null &&
            (latestStartAt == null || latestEndAt > latestStartAt)

    val currentInvitations =
        if (locallyInactive) {
            invitations.filter { invitation ->
                invitation.createdAtEpochMilliseconds > requireNotNull(latestEndAt)
            }
        } else {
            invitations
        }

    return GroupLocalMembershipTimeline(
        visibleMessages = orderedMessages.visibleDuringMembershipPeriods(),
        currentInvitations = currentInvitations,
        isLocallyInactive = locallyInactive && currentInvitations.isEmpty()
    )
}

private fun List<MessageEntity>.visibleDuringMembershipPeriods(): List<MessageEntity> {
    var active = true
    return buildList {
        this@visibleDuringMembershipPeriods.forEach { message ->
            when {
                GroupMembershipMessageFactory.isLocalMembershipStart(message.transportMode) -> {
                    active = true
                }

                GroupMembershipMessageFactory.isLocalMembershipEnd(message.transportMode) -> {
                    if (active) add(message)
                    active = false
                }

                GroupMembershipMessageFactory.isHiddenControlMessage(message.transportMode) -> Unit
                active -> add(message)
            }
        }
    }
}
