package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberLifecycleSnapshot

internal data class GroupLocalMembershipTimelineDto(
    val visibleMessages: List<MessageEntity>,
    val currentMemberships: List<GroupMemberLifecycleSnapshot>,
    val isLocallyInactive: Boolean
)

internal fun buildGroupLocalMembershipTimeline(
    messages: List<MessageEntity>,
    memberships: List<GroupMemberLifecycleSnapshot>,
    localMembershipHistory: List<MessageEntity> = messages
): GroupLocalMembershipTimelineDto {
    val orderedMessages = messages.sortedWith(MESSAGE_ORDER)
    val orderedMembershipHistory = localMembershipHistory.sortedWith(MESSAGE_ORDER)
    val latestBoundary =
        orderedMembershipHistory
            .filter { message -> message.isLocalMembershipBoundary() }
            .maxWithOrNull(MESSAGE_ORDER)
    val locallyInactive =
        latestBoundary?.let { message ->
            GroupMembershipMessageFactory.isLocalMembershipEnd(message.transportMode)
        } == true
    val latestEndAt =
        orderedMembershipHistory
            .filter { message -> GroupMembershipMessageFactory.isLocalMembershipEnd(message.transportMode) }
            .maxWithOrNull(MESSAGE_ORDER)
            ?.createdAtEpochMilliseconds

    val currentMemberships =
        if (locallyInactive && latestEndAt != null) {
            memberships.filter { membership ->
                membership.createdAtEpochMilliseconds > latestEndAt
            }
        } else {
            memberships
        }

    val initiallyActive =
        orderedMessages
            .firstOrNull()
            ?.let { firstLoadedMessage ->
                orderedMembershipHistory
                    .asSequence()
                    .filter { message -> message.isLocalMembershipBoundary() }
                    .filter { message -> MESSAGE_ORDER.compare(message, firstLoadedMessage) < 0 }
                    .maxWithOrNull(MESSAGE_ORDER)
                    ?.let { previousBoundary ->
                        !GroupMembershipMessageFactory.isLocalMembershipEnd(previousBoundary.transportMode)
                    }
            } ?: true

    return GroupLocalMembershipTimelineDto(
        visibleMessages = orderedMessages.visibleDuringMembershipPeriods(initiallyActive),
        currentMemberships = currentMemberships,
        isLocallyInactive = locallyInactive && currentMemberships.isEmpty()
    )
}

private fun List<MessageEntity>.visibleDuringMembershipPeriods(initiallyActive: Boolean): List<MessageEntity> {
    var active = initiallyActive
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

private fun MessageEntity.isLocalMembershipBoundary(): Boolean =
    GroupMembershipMessageFactory.isLocalMembershipStart(transportMode) ||
        GroupMembershipMessageFactory.isLocalMembershipEnd(transportMode)

private val MESSAGE_ORDER =
    compareBy<MessageEntity>(MessageEntity::createdAtEpochMilliseconds)
        .thenBy(MessageEntity::id)
