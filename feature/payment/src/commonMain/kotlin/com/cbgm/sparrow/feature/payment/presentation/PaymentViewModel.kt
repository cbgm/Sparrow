package com.cbgm.sparrow.feature.payment.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.flatMap
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.payment.domain.usecase.DeleteBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.FindBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.ObservePaymentSessionUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.PreparePaymentUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.RecordSelectedPaymentUseCase
import com.cbgm.sparrow.feature.payment.presentation.mapper.toBankDetails
import com.cbgm.sparrow.feature.payment.presentation.mapper.toDomain
import com.cbgm.sparrow.feature.payment.presentation.mapper.toImageRequest
import com.cbgm.sparrow.feature.payment.presentation.mapper.toPaymentErrorUi
import com.cbgm.sparrow.feature.payment.presentation.mapper.toPaymentSelectionUi
import com.cbgm.sparrow.feature.payment.presentation.mapper.toStringResource
import com.cbgm.sparrow.feature.payment.presentation.mapper.toTransferStatus
import com.cbgm.sparrow.feature.payment.presentation.mapper.toUi
import com.cbgm.sparrow.feature.payment.presentation.mapper.toUiState
import com.cbgm.sparrow.feature.payment.presentation.mapper.unavailable
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentErrorUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentReviewUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentSessionUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentStatusUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiEvent
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class PaymentViewModel(
    savedStateHandle: SavedStateHandle,
    private val observeSession: ObservePaymentSessionUseCase,
    private val preparePayment: PreparePaymentUseCase,
    private val recordPayment: RecordSelectedPaymentUseCase,
    private val findBankDetails: FindBankDetailsUseCase,
    private val deleteBankDetails: DeleteBankDetailsUseCase
) : BaseViewModel() {
    private val selection = savedStateHandle.toPaymentSelectionUi()
    private var session: PaymentSessionUi? = null
    private var review: PaymentReviewUi? = null
    private var detailsLoaded = false
    private var requestId = 0
    private var operation: Job? = null
    private var operationId = 0L

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession(selection.toDomain())
                .map { it?.toUi() }
                .distinctUntilChanged()
                .catch {
                    invalidate()
                    report(PaymentErrorUi.LOAD)
                }
                .collect { current ->
                    if (current == null) {
                        invalidate()
                        report(PaymentErrorUi.STALE)
                    } else {
                        applySession(current)
                    }
                }
        }
    }

    fun onUiEvent(event: PaymentUiEvent) {
        when (event) {
            PaymentUiEvent.Back -> {
                navigator.popBackStack()
                return
            }
            is PaymentUiEvent.ShareCompleted -> {
                _uiState.update { it.copy(imageRequest = null) }
                if (!event.success) report(PaymentErrorUi.SHARE)
                return
            }
            else -> Unit
        }

        if (_uiState.value.isLoading || _uiState.value.isBusy) return

        when (event) {
            is PaymentUiEvent.HolderChanged -> _uiState.update {
                it.copy(accountHolder = event.value.take(70), isReviewing = false)
            }
            is PaymentUiEvent.IbanChanged -> _uiState.update {
                it.copy(iban = event.value.take(50), isReviewing = false)
            }
            is PaymentUiEvent.RememberChanged -> _uiState.update {
                it.copy(rememberDetails = event.value)
            }
            PaymentUiEvent.Review -> prepareReview()
            PaymentUiEvent.Edit -> {
                review = null
                _uiState.update { it.copy(isReviewing = false, imageRequest = null) }
            }
            PaymentUiEvent.Share -> share()
            PaymentUiEvent.ForgetDetails -> forgetDetails()
            PaymentUiEvent.MarkSent -> record(PaymentStatusUi.SENT)
            PaymentUiEvent.ConfirmReceipt -> record(PaymentStatusUi.RECEIVED)
        }
    }

    private fun applySession(current: PaymentSessionUi) {
        val becameStale = session?.canPay == true && !current.canPay && current.status == PaymentStatusUi.UNPAID
        session = current
        if (becameStale) operation?.cancel()
        if (!current.canPay) review = null
        _uiState.update { current.toUiState(it) }
        if (current.canPay && !detailsLoaded) loadDetails(current.bankDetailsKey)
    }

    private fun loadDetails(key: String) {
        detailsLoaded = true
        runAction(
            fallback = PaymentErrorUi.LOAD,
            action = { findBankDetails(key).map { it?.toUi() } }
        ) { saved ->
            if (saved != null && session?.canPay == true) {
                _uiState.update {
                    it.copy(
                        accountHolder = saved.accountHolder,
                        iban = saved.iban,
                        rememberDetails = true,
                        hasSavedDetails = true
                    )
                }
            }
        }
    }

    private fun prepareReview() {
        val draft = _uiState.value
        runAction(
            fallback = PaymentErrorUi.SAVE,
            action = {
                preparePayment(selection.toDomain(), draft.toBankDetails(), draft.rememberDetails).map { it.toUi() }
            }
        ) { prepared ->
            if (session?.canPay != true) {
                report(PaymentErrorUi.STALE)
                return@runAction
            }
            review = prepared
            _uiState.update {
                it.copy(
                    accountHolder = prepared.accountHolder,
                    iban = prepared.iban,
                    isReviewing = true,
                    hasSavedDetails = draft.rememberDetails
                )
            }
        }
    }

    private fun forgetDetails() {
        val key = session?.bankDetailsKey ?: return
        runAction(
            fallback = PaymentErrorUi.SAVE,
            action = { deleteBankDetails(key) }
        ) {
            review = null
            _uiState.update {
                it.copy(
                    accountHolder = "",
                    iban = "",
                    hasSavedDetails = false,
                    rememberDetails = false,
                    isReviewing = false,
                    imageRequest = null
                )
            }
        }
    }

    private fun share() {
        val prepared = review ?: return
        if (!_uiState.value.isReviewing || session?.canPay != true) {
            report(PaymentErrorUi.STALE)
            return
        }
        _uiState.update { it.copy(imageRequest = prepared.toImageRequest(++requestId)) }
    }

    private fun record(status: PaymentStatusUi) {
        if (status == PaymentStatusUi.SENT && !_uiState.value.isReviewing) return
        runAction(
            fallback = PaymentErrorUi.SEND,
            action = { recordPayment(selection.toDomain(), status.toTransferStatus()) }
        ) {
            review = null
            _uiState.update { it.copy(accountHolder = "", iban = "", imageRequest = null) }
        }
    }

    private fun <T> runAction(
        fallback: PaymentErrorUi,
        action: suspend () -> Result<T>,
        onSuccess: (T) -> Unit
    ) {
        if (_uiState.value.isBusy) return
        val id = ++operationId
        _uiState.update { it.copy(isBusy = true) }
        operation = viewModelScope.launch {
            try {
                val result = safeSuspendCall(logFailure = false) { action() }.flatMap { it }
                currentCoroutineContext().ensureActive()
                result.onSuccess(onSuccess).onFailure { report(it.toPaymentErrorUi(fallback)) }
            } finally {
                if (id == operationId) _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun report(error: PaymentErrorUi) {
        viewModelScope.launch {
            SparrowLog.showError(getString(error.toStringResource()))
        }
    }

    private fun invalidate() {
        operationId++
        operation?.cancel()
        session = null
        review = null
        _uiState.update { it.unavailable() }
    }

    override fun onCleared() {
        operationId++
        operation?.cancel()
        session = null
        review = null
        _uiState.value = PaymentUiState()
    }
}
