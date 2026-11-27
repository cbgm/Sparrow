package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/**
 * A group has one pin slot. An open expense board holds it exclusively until the board is closed,
 * so neither a local admin nor a remote one can pin another message over it.
 */
object GroupPinSlotPolicy {
    /** Whether an open expense board holds the slot of [pin]. */
    fun isHeldByExpenses(pin: GroupPinState?): Boolean = pin?.hasActiveExpenseBoard == true

    /**
     * Whether pinning [messageId] must yield to the open expense board in [current]. Clearing the slot (null) and
     * pinning the board message itself again are not blocked.
     */
    fun expensesKeepSlot(current: GroupPinState?, messageId: String?): Boolean =
        isHeldByExpenses(current) && messageId != null && messageId != current?.messageId

    /** A group has at most one open expense board. */
    fun checkNoActiveExpenseBoard(pin: GroupPinState?) {
        check(!isHeldByExpenses(pin)) { "Group expenses are already active" }
    }
}
