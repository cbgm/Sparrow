package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentSettlementBalanceTest {
    private val board = ExpenseBoard("board", "EUR", 1L)
    private val expense = Expense("expense", "board", "Dinner", 10000L, "EUR", "recipient", listOf(ExpenseAllocation("payer", 10000L)), 1L)
    private val sent = PaymentTransfer("transfer-sent", "transfer", "board", "payer", "recipient", 10000L, "EUR", "SPARROW-test", PaymentTransferStatus.SENT, 2L)
    private val receipt = sent.copy(id = "transfer-received", status = PaymentTransferStatus.RECEIVED)

    private fun balances(events: List<PaymentTransfer>) = CalculateExpenseBalancesUseCase()(board, listOf(expense), events)
        .getOrThrow().associate { it.memberId to it.amountMinor }

    @Test fun sentAloneDoesNotReduceDebt() {
        assertEquals(-10000L, balances(listOf(sent))["payer"])
    }

    @Test fun recipientConfirmationSettlesOnceDespiteDuplicateEvents() {
        val values = balances(listOf(sent, receipt, sent, receipt))
        assertEquals(0L, values["payer"])
        assertEquals(0L, values["recipient"])
    }

    @Test fun mismatchedConfirmationDoesNotSettle() {
        assertEquals(-10000L, balances(listOf(sent, receipt.copy(amountMinor = 999L)))["payer"])
    }

    @Test fun receiptCanArriveBeforeSentAndStillConverges() {
        assertEquals(0L, balances(listOf(receipt, sent))["payer"])
        assertEquals(-10000L, balances(listOf(receipt))["payer"])
    }

    @Test fun unrelatedBoardsCannotChangeBalances() {
        assertEquals(-10000L, balances(listOf(sent.copy(boardId = "other"), receipt.copy(boardId = "other")))["payer"])
    }
}
