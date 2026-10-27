package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

/** What the conversation row with a given group ID is, if there is one. */
enum class GroupConversationKind {
    NONE,
    GROUP,

    /** A direct conversation or anything else that is not a group. */
    OTHER
}
