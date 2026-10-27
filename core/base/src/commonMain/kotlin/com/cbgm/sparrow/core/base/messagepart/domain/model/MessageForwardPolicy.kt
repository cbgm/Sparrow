package com.cbgm.sparrow.core.base.messagepart.domain.model

/** Which message parts may be forwarded to another conversation. */
object MessageForwardPolicy {
    fun requireForwardable(parts: List<MessagePart>) {
        require(parts.isNotEmpty()) { "Message has no forwardable content" }
        require(parts.all(::isForwardable)) { "Polls and expenses cannot be forwarded" }
    }

    /** Payments, polls and expenses belong to the group they were created in. */
    fun isForwardable(part: MessagePart): Boolean =
        part !is PaymentTransfer &&
            part !is Poll &&
            part !is Expense &&
            part !is ExpenseBoard &&
            part !is ExpenseBoardClosed
}
