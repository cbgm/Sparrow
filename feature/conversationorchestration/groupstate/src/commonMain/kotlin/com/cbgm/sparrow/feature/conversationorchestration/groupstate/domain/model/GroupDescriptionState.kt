package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/** The group description and when an admin last changed it; 0 means it was never published. */
data class GroupDescriptionState(
    val groupId: String,
    val description: String?,
    val changedAtEpochMilliseconds: Long
) {
    val isPublished: Boolean get() = changedAtEpochMilliseconds > 0L
}
