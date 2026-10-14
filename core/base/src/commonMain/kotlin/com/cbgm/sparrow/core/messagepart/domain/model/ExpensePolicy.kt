package com.cbgm.sparrow.core.messagepart.domain.model

object ExpensePolicy {
    const val MAX_DESCRIPTION_LENGTH = 500
    const val MAX_ALLOCATIONS = 256
    const val MAX_RECEIPT_BYTES = 1_048_576
    const val MAX_RECEIPT_DIMENSION = 2_048

    fun requireValid(board: ExpenseBoard) {
        require(board.id.isNotBlank()) { "Expense board ID must not be blank" }
        requireCurrency(board.currencyCode)
        require(board.activatedAtEpochMilliseconds > 0L) { "Invalid board activation time" }
        require(board.closedAtEpochMilliseconds == null || board.closedAtEpochMilliseconds >= board.activatedAtEpochMilliseconds) {
            "Board closure precedes activation"
        }
    }

    fun requireValid(event: ExpenseBoardClosed) {
        require(event.id.isNotBlank()) { "Expense closure ID must not be blank" }
        require(event.boardId.isNotBlank() && event.boardId != event.id) { "Invalid closed board ID" }
        require(event.closedAtEpochMilliseconds > 0L) { "Invalid closing time" }
    }

    fun requireValid(expense: Expense) {
        require(expense.id.isNotBlank()) { "Expense ID must not be blank" }
        require(expense.boardId.isNotBlank() && expense.boardId != expense.id) { "Invalid expense board reference" }
        require(expense.description.isNotBlank() && expense.description.length <= MAX_DESCRIPTION_LENGTH) {
            "Invalid expense description"
        }
        require(expense.amountMinor > 0L) { "Expense amount must be positive" }
        requireCurrency(expense.currencyCode)
        require(expense.paidByMemberId.isNotBlank()) { "Expense payer is missing" }
        require(expense.occurredAtEpochMilliseconds > 0L) { "Invalid expense date" }
        require(expense.editedAtEpochMilliseconds == null || expense.editedAtEpochMilliseconds > 0L) {
            "Invalid expense edit time"
        }
        require(expense.deletedAtEpochMilliseconds == null || expense.deletedAtEpochMilliseconds > 0L) {
            "Invalid expense deletion time"
        }
        require(expense.revisionOf != expense.id) { "An expense cannot revise itself" }
        require(
            (expense.revisionOf != null) ==
                (expense.editedAtEpochMilliseconds != null || expense.deletedAtEpochMilliseconds != null)
        ) { "Only expense revisions are edited or deleted" }
        require(expense.allocations.size in 1..MAX_ALLOCATIONS) { "Invalid number of expense participants" }
        require(expense.allocations.map(ExpenseAllocation::memberId).distinct().size == expense.allocations.size) {
            "Expense participants must be unique"
        }
        var allocated = 0L
        expense.allocations.forEach { share ->
            require(share.memberId.isNotBlank()) { "Expense participant is missing" }
            require(share.amountMinor > 0L && share.amountMinor <= expense.amountMinor - allocated) {
                "Invalid expense allocation"
            }
            allocated += share.amountMinor
        }
        require(allocated == expense.amountMinor) { "Expense allocations must equal the full amount" }
        expense.receipt?.let { receipt ->
            require(receipt.id.isNotBlank()) { "Expense receipt ID must not be blank" }
            require(receipt.id != expense.id && receipt.id != expense.boardId) {
                "Receipt and expense IDs must differ"
            }
            require(receipt.mimeType.startsWith("image/")) { "Expense receipt must be an image" }
            require((receipt.width ?: 1) > 0 && (receipt.height ?: 1) > 0) {
                "Invalid expense receipt dimensions"
            }
            require(receipt.byteSize in 1L..MAX_RECEIPT_BYTES.toLong()) { "Invalid expense receipt size" }
            require(receipt.bytes == null || receipt.bytes.size.toLong() == receipt.byteSize) {
                "Expense receipt size does not match"
            }
        }
    }

    private fun requireCurrency(code: String) {
        require(code.length == 3 && code.all { it in 'A'..'Z' }) {
            "Currency must be an uppercase three-letter ISO 4217 code"
        }
    }
}
