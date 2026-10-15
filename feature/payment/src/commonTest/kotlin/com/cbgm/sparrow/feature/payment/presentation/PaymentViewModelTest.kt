package com.cbgm.sparrow.feature.payment.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.payment.domain.usecase.DeleteBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.FindBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.PaymentFixture
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiEvent
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_payment_invalid_details
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    @Test
    fun repeatedValidationErrorsUseGlobalFeedbackAndReleaseBusyState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = PaymentFixture()
        fixture.publish()
        val viewModel = fixture.viewModel()
        try {
            runCurrent()
            val expected = getString(Res.string.feature_payment_invalid_details)
            val errors = mutableListOf<String>()
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                SparrowLog.errors.filter { it == expected }.take(2).toList(errors)
            }
            viewModel.onUiEvent(PaymentUiEvent.HolderChanged("Recipient"))
            viewModel.onUiEvent(PaymentUiEvent.IbanChanged("invalid"))
            repeat(2) {
                viewModel.onUiEvent(PaymentUiEvent.Review)
                runCurrent()
                assertFalse(viewModel.uiState.value.isBusy)
                assertFalse(viewModel.uiState.value.isReviewing)
            }
            collector.join()
            assertEquals(listOf(expected, expected), errors)
        } finally {
            viewModel.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun contextInvalidationClearsPreparedBankDetailsAndShareRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = PaymentFixture()
        fixture.publish()
        val viewModel = fixture.viewModel()
        try {
            runCurrent()
            viewModel.onUiEvent(PaymentUiEvent.HolderChanged(fixture.bank.accountHolder))
            viewModel.onUiEvent(PaymentUiEvent.IbanChanged(fixture.bank.iban))
            viewModel.onUiEvent(PaymentUiEvent.Review)
            runCurrent()
            assertTrue(viewModel.uiState.value.isReviewing)
            viewModel.onUiEvent(PaymentUiEvent.Share)
            assertTrue(viewModel.uiState.value.imageRequest != null)
            fixture.composer.clearContext("group")
            runCurrent()
            val state = viewModel.uiState.value
            assertFalse(state.canPay)
            assertFalse(state.isBusy)
            assertFalse(state.isReviewing)
            assertEquals("", state.accountHolder)
            assertEquals("", state.iban)
            assertEquals(null, state.imageRequest)
        } finally {
            viewModel.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun stateStringShowsSafeStatusWithoutBankDetails() {
        val state = PaymentUiState(isLoading = false, accountHolder = "Secret Holder", iban = "Secret IBAN")
        val text = state.toString()
        assertTrue(text.contains("isLoading=false"))
        assertTrue(text.contains("status=UNPAID"))
        assertFalse(text.contains("Secret"))
    }

    // This private commonTest factory intentionally uses the test-only constructor.
    @Suppress("VisibleForTests")
    private fun PaymentFixture.viewModel() = PaymentViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "groupId" to selection.groupId,
                "boardId" to selection.boardId,
                "fromMemberId" to selection.fromMemberId,
                "toMemberId" to selection.toMemberId,
                "amountMinor" to selection.amountMinor,
                "transferId" to selection.transferId
            )
        ),
        observeSession = observe,
        preparePayment = prepare,
        recordPayment = record,
        findBankDetails = FindBankDetailsUseCase(banks),
        deleteBankDetails = DeleteBankDetailsUseCase(banks)
    )
}
