package com.cbgm.sparrow.feature.expenses.presentation.create.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseCategory
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseConversion
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseSplit
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.feature.expenses.domain.ExpenseCurrencies
import com.cbgm.sparrow.feature.expenses.domain.ExpenseMoney
import com.cbgm.sparrow.feature.expenses.domain.model.ExchangeRates
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseCreationContext
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseMember
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCurrencyUi
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
        recentCurrencyCodes = recentCurrencyCodes,
        participants = participants,
        draft = expense?.toExpenseDraftUi() ?: ExpenseDraftUi(
            payerId = localMemberId,
            participantIds = participants.mapTo(mutableSetOf()) { it.id },
            currencyCode = board.currencyCode,
            baseCurrencyCode = board.currencyCode
        ),
        isLoading = false,
        isEditing = expense != null
    )
}

private fun Expense.toExpenseDraftUi(): ExpenseDraftUi {
    val participantIds = allocations.mapTo(mutableSetOf(), ExpenseAllocation::memberId)
    val isEqualSplit = ExpenseSplit.evenly(amountMinor, participantIds) == allocations.sortedBy(ExpenseAllocation::memberId)
    val currency = conversion?.originalCurrencyCode ?: currencyCode
    val total = conversion?.originalAmountMinor ?: amountMinor
    return ExpenseDraftUi(
        description = description,
        amount = ExpenseMoney.toInput(total, currency),
        payerId = paidByMemberId,
        participantIds = participantIds,
        category = ExpenseCategoryUi.valueOf(category.name),
        splitMode = if (isEqualSplit) ExpenseSplitModeUi.EQUAL else ExpenseSplitModeUi.CUSTOM,
        customAmounts = if (isEqualSplit) {
            emptyMap()
        } else {
            originalShares().mapValues { (_, share) ->
                ExpenseMoney.toInput(share, currency)
            }
        },
        receipt = receipt?.toReceiptSelection(),
        currencyCode = currency,
        baseCurrencyCode = currencyCode,
        rate = conversion?.rate
    )
}

private fun Expense.originalShares(): Map<String, Long> {
    val shares = allocations.associate { it.memberId to it.amountMinor }
    val conversion = conversion ?: return shares
    val inverseRate = ExpenseMoney.formatRate(1.0 / conversion.rate.toDouble())
    return ExpenseMoney.convertShares(
        shares = shares,
        totalMinor = conversion.originalAmountMinor,
        fromCurrencyCode = currencyCode,
        toCurrencyCode = conversion.originalCurrencyCode,
        rate = inverseRate
    ) ?: shares
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
    val amount = baseTotalMinor ?: return null
    val allocations = toExpenseAllocations() ?: return null
    return original.copy(
        description = description.trim(),
        amountMinor = amount,
        conversion = toExpenseConversion(),
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
    baseAllocationsMinor()?.map { (memberId, amountMinor) ->
        ExpenseAllocation(memberId, amountMinor)
    }

internal fun ExpenseDraftUi.toExpenseConversion(): ExpenseConversion? {
    if (!isConverted) return null
    return ExpenseConversion(
        originalAmountMinor = totalMinor ?: return null,
        originalCurrencyCode = currencyCode,
        rate = rate ?: return null
    )
}

internal fun ExpenseDraftUi.withCurrency(currencyCode: String, rate: String?): ExpenseDraftUi =
    copy(currencyCode = currencyCode, rate = rate.takeIf { currencyCode != baseCurrencyCode })

internal fun ExchangeRates?.toExpenseCurrencyUi(
    baseCurrencyCode: String,
    recentCurrencyCodes: List<String>
): List<ExpenseCurrencyUi> {
    val pinned = (listOf(baseCurrencyCode) + recentCurrencyCodes).distinct()
    val codes = (pinned + this?.currencyCodes.orEmpty()).distinct()
    return codes.map { code ->
        ExpenseCurrencyUi(
            code = code,
            symbol = ExpenseCurrencies.symbol(code),
            isAvailable = code == baseCurrencyCode || this?.rate(code, baseCurrencyCode) != null,
            isPinned = code in pinned
        )
    }
}

internal fun ExchangeRates.rateText(fromCurrencyCode: String, toCurrencyCode: String): String? =
    rate(fromCurrencyCode, toCurrencyCode)?.let(ExpenseMoney::formatRate)

/** The next draft keeps payer, participants, category and split mode; custom amounts depend on the amount. */
internal fun ExpenseDraftUi.nextDraft(): ExpenseDraftUi =
    ExpenseDraftUi(
        payerId = payerId,
        participantIds = participantIds,
        category = category,
        splitMode = splitMode,
        currencyCode = currencyCode,
        baseCurrencyCode = baseCurrencyCode,
        rate = rate
    )

/** Switching to a custom split starts from the equal shares, so the user only adjusts the differences. */
internal fun ExpenseDraftUi.withSplitMode(mode: ExpenseSplitModeUi): ExpenseDraftUi {
    if (mode == splitMode) return this
    if (mode == ExpenseSplitModeUi.EQUAL) return copy(splitMode = mode, customAmounts = emptyMap())
    val equalShares = copy(splitMode = ExpenseSplitModeUi.EQUAL).allocationsMinor().orEmpty()
    return copy(
        splitMode = mode,
        customAmounts = equalShares.mapValues { (_, amountMinor) ->
            ExpenseMoney.toInput(amountMinor, currencyCode, if (',' in amount) ',' else '.')
        }
    )
}

internal fun ExpenseDraftUi.withReceiptScan(scan: ReceiptScan): ExpenseDraftUi {
    val newAmount = scan.totalMinor?.toScannedAmountInput(currencyCode).orEmpty()
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

private fun Long.toScannedAmountInput(currencyCode: String): String =
    when (ExpenseCurrencies.fractionDigits(currencyCode)) {
        0 -> (this / 100).toString()
        else -> "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
    }
