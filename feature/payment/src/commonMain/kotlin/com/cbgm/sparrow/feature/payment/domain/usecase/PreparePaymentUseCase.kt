package com.cbgm.sparrow.feature.payment.domain.usecase

import com.cbgm.sparrow.core.util.result.flatMap
import com.cbgm.sparrow.core.util.result.safeCall
import com.cbgm.sparrow.feature.payment.domain.SepaPaymentPolicy
import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.model.InvalidPaymentDetailsException
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSelection
import com.cbgm.sparrow.feature.payment.domain.model.PaymentUnavailableException
import com.cbgm.sparrow.feature.payment.domain.model.PreparedPayment
import com.cbgm.sparrow.feature.payment.domain.model.SepaPayment
import kotlinx.coroutines.flow.first

class PreparePaymentUseCase(
    private val observeSession: ObservePaymentSessionUseCase,
    private val saveBankDetails: SaveBankDetailsUseCase,
    private val deleteBankDetails: DeleteBankDetailsUseCase
) {
    suspend operator fun invoke(
        selection: PaymentSelection,
        bankDetails: BankDetails,
        rememberDetails: Boolean
    ): Result<PreparedPayment> {
        val session = observeSession(selection).first()
        if (session?.canPay != true) return Result.failure(PaymentUnavailableException())

        val prepared = safeCall(logFailure = false) {
            val normalized = SepaPaymentPolicy.normalize(bankDetails)
            val payment = SepaPayment(normalized, selection.amountMinor, session.reference)
            PreparedPayment(
                bankDetails = normalized,
                amount = SepaPaymentPolicy.amount(selection.amountMinor),
                reference = session.reference,
                epcPayload = SepaPaymentPolicy.epcPayload(payment)
            )
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(InvalidPaymentDetailsException()) }
        )
        return prepared.flatMap { payment ->
            val stored = if (rememberDetails) {
                saveBankDetails(session.bankDetailsKey, payment.bankDetails)
            } else {
                deleteBankDetails(session.bankDetailsKey)
            }
            stored.map { payment }
        }
    }
}
