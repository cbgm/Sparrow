package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/**
 * The group title and when an admin last changed it; 0 means it was never published. [publishPending] marks a
 * local change that is shown already but not queued for the members yet.
 */
data class GroupTitleState(
    val groupId: String,
    val title: String,
    val changedAtEpochMilliseconds: Long,
    val publishPending: Boolean = false
) {
    val isPublished: Boolean get() = changedAtEpochMilliseconds > 0L
}
