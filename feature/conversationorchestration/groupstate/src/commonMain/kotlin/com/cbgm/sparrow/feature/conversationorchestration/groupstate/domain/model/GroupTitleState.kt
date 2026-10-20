package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/** The group title and when an admin last changed it; 0 means it was never published. */
data class GroupTitleState(
    val groupId: String,
    val title: String,
    val changedAtEpochMilliseconds: Long
) {
    val isPublished: Boolean get() = changedAtEpochMilliseconds > 0L
}
