package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/** The last-writer-wins group states an admin publishes to the members. */
enum class GroupStateKind {
    TITLE,
    DESCRIPTION,
    AVATAR,
    PIN
}

/** A group state of [kind] that was saved locally but not queued for the members of [groupId] yet. */
data class UnpublishedGroupState(
    val groupId: String,
    val kind: GroupStateKind
)
