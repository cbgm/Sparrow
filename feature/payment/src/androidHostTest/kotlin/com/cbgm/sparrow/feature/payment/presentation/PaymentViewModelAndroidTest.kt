package com.cbgm.sparrow.feature.payment.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.payment.domain.usecase.DeleteBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.FindBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.PaymentFixture
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiEvent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_payment_invalid_details
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelAndroidTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun repeatedValidationErrorsUseGlobalFeedbackAndReleaseBusyState() = runTest {
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
        }
    }

    @Test
    fun contextInvalidationClearsPreparedBankDetailsAndShareRequest() = runTest {
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
        }
    }

    @Suppress("VisibleForTests")
    private fun PaymentFixture.viewModel() = PaymentViewModel(
        savedStateHandle = SavedStateHandle(mapOf(
            "groupId" to selection.groupId,
            "boardId" to selection.boardId,
            "fromMemberId" to selection.fromMemberId,
            "toMemberId" to selection.toMemberId,
            "amountMinor" to selection.amountMinor,
            "transferId" to selection.transferId
        )),
        observeSession = observe,
        preparePayment = prepare,
        recordPayment = record,
        findBankDetails = FindBankDetailsUseCase(banks),
        deleteBankDetails = DeleteBankDetailsUseCase(banks)
    )
}
