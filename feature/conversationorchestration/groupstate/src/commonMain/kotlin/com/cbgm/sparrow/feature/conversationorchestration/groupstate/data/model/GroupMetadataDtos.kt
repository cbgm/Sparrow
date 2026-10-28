package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model

internal data class GroupTitleDto(
    val title: String,
    val changedAtEpochMilliseconds: Long,
    val publishPending: Boolean
)

internal data class GroupDescriptionDto(
    val description: String?,
    val changedAtEpochMilliseconds: Long,
    val publishPending: Boolean
)

internal class GroupAvatarDto(
    val bytes: ByteArray?,
    val changedAtEpochMilliseconds: Long,
    val publishPending: Boolean
)
