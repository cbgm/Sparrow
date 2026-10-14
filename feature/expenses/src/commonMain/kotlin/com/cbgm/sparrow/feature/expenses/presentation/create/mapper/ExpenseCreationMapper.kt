package com.cbgm.sparrow.feature.expenses.presentation.create.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseCategory
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseSplit
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseCreationContext
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseMember
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseSplitModeUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.feature.ocr.domain.model.ReceiptScan

internal fun ExpenseCreationContext.toCreateExpenseUiState(): CreateExpenseUiState {
    val participants = members.map { member -> member.toExpenseParticipantUi() }
    return CreateExpenseUiState(
        boardId = board.id,
        currencyCode = board.currencyCode,
        participants = participants,
        draft = expense?.toExpenseDraftUi() ?: ExpenseDraftUi(
            payerId = localMemberId,
            participantIds = participants.mapTo(mutableSetOf()) { it.id }
        ),
        isLoading = false,
        isEditing = expense != null
    )
}

private fun Expense.toExpenseDraftUi(): ExpenseDraftUi {
    val participantIds = allocations.mapTo(mutableSetOf(), ExpenseAllocation::memberId)
    val isEqualSplit = ExpenseSplit.evenly(amountMinor, participantIds) == allocations.sortedBy(ExpenseAllocation::memberId)
    return ExpenseDraftUi(
        description = description,
        amount = amountMinor.toAmountText('.'),
        payerId = paidByMemberId,
        participantIds = participantIds,
        category = ExpenseCategoryUi.valueOf(category.name),
        splitMode = if (isEqualSplit) ExpenseSplitModeUi.EQUAL else ExpenseSplitModeUi.CUSTOM,
        customAmounts = if (isEqualSplit) emptyMap() else allocations.associate { it.memberId to it.amountMinor.toAmountText('.') },
        receipt = receipt?.toReceiptSelection()
    )
}

private fun Image.toReceiptSelection(): VisualMediaSelectionUi? =
    localFilePath?.let { path ->
        VisualMediaSelectionUi(
            id = id,
            localFilePath = path,
            byteSize = byteSize,
            mimeType = mimeType,
            source = MediaSourceUi.CAMERA,
            type = MediaTypeUi.IMAGE,
            width = width,
            height = height
        )
    }

internal fun ExpenseDraftUi.toEditedExpense(original: Expense): Expense? {
    val amount = totalMinor ?: return null
    val allocations = toExpenseAllocations() ?: return null
    return original.copy(
        description = description.trim(),
        amountMinor = amount,
        paidByMemberId = payerId,
        allocations = allocations,
        category = category.toExpenseCategory(),
        receipt = receipt?.let { selection ->
            original.receipt?.takeIf { it.id == selection.id } ?: selection.toReceiptImage()
        }
    )
}

internal fun VisualMediaSelectionUi.toReceiptImage(): Image =
    Image(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        width = width,
        height = height,
        localFilePath = localFilePath
    )

internal fun ExpenseMember.toExpenseParticipantUi(): ExpenseParticipantUi =
    ExpenseParticipantUi(id, displayName, isLocal, avatarContactId)

internal fun ExpenseCategoryUi.toExpenseCategory(): ExpenseCategory = ExpenseCategory.valueOf(name)

internal fun ExpenseDraftUi.toExpenseAllocations(): List<ExpenseAllocation>? =
    allocationsMinor()?.map { (memberId, amountMinor) -> ExpenseAllocation(memberId, amountMinor) }

/** The next draft keeps payer, participants, category and split mode; custom amounts depend on the amount. */
internal fun ExpenseDraftUi.nextDraft(): ExpenseDraftUi =
    ExpenseDraftUi(
        payerId = payerId,
        participantIds = participantIds,
        category = category,
        splitMode = splitMode
    )

/** Switching to a custom split starts from the equal shares, so the user only adjusts the differences. */
internal fun ExpenseDraftUi.withSplitMode(mode: ExpenseSplitModeUi): ExpenseDraftUi {
    if (mode == splitMode) return this
    if (mode == ExpenseSplitModeUi.EQUAL) return copy(splitMode = mode, customAmounts = emptyMap())
    val equalShares = copy(splitMode = ExpenseSplitModeUi.EQUAL).allocationsMinor().orEmpty()
    return copy(
        splitMode = mode,
        customAmounts = equalShares.mapValues { (_, amountMinor) -> amountMinor.toAmountText(if (',' in amount) ',' else '.') }
    )
}

internal fun ExpenseDraftUi.withReceiptScan(scan: ReceiptScan): ExpenseDraftUi {
    val newAmount = scan.totalMinor?.toAmountText('.').orEmpty()
    val newDescription = scan.merchant.orEmpty()
    val replaceAmount = amount.isBlank() || amount == scannedAmount
    val replaceDescription = description.isBlank() || description == scannedDescription
    return copy(
        amount = if (replaceAmount) newAmount else amount,
        description = if (replaceDescription) newDescription else description,
        scannedAmount = newAmount,
        scannedDescription = newDescription
    )
}

internal fun String.toAmountInput(): String = filter { it.isDigit() || it == '.' || it == ',' }.take(24)

private fun Long.toAmountText(separator: Char): String = "${this / 100}$separator${(this % 100).toString().padStart(2, '0')}"
