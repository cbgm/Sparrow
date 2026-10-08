package com.cbgm.sparrow.core.messagepart.domain.model

object ExpensePolicy {
    const val MAX_DESCRIPTION_LENGTH = 500
    const val MAX_ALLOCATIONS = 256

    fun requireValid(board: ExpenseBoard) {
        require(board.id.isNotBlank()) { "Expense board ID must not be blank" }
        requireCurrency(board.currencyCode)
        require(board.activatedAtEpochMilliseconds > 0L) { "Invalid board activation time" }
        require(board.closedAtEpochMilliseconds == null || board.closedAtEpochMilliseconds >= board.activatedAtEpochMilliseconds) {
            "Board closure precedes activation"
        }
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
        expense.receipt?.let { image ->
            require(image.id != expense.id && image.id != expense.boardId) { "Receipt and expense IDs must differ" }
            MessageAttachmentPolicy.requireValid(listOf(image))
            require(image.byteSize in 1L..MessageAttachmentPolicy.MAX_IMAGE_BYTES.toLong()) {
                "Invalid expense receipt size"
            }
        }
    }

    private fun requireCurrency(code: String) {
        require(code.length == 3 && code.all { it in 'A'..'Z' }) {
            "Currency must be an uppercase three-letter ISO 4217 code"
        }
    }
}
