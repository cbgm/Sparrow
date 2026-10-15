package com.cbgm.sparrow.feature.payment.presentation.mapper

import com.cbgm.sparrow.feature.payment.domain.model.InvalidPaymentDetailsException
import com.cbgm.sparrow.feature.payment.domain.model.PaymentUnavailableException
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentErrorUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_payment_invalid_details
import com.cbgm.sparrow.resources.feature_payment_load_failed
import com.cbgm.sparrow.resources.feature_payment_save_failed
import com.cbgm.sparrow.resources.feature_payment_send_failed
import com.cbgm.sparrow.resources.feature_payment_share_failed
import com.cbgm.sparrow.resources.feature_payment_stale
import org.jetbrains.compose.resources.StringResource

internal fun PaymentErrorUi.toStringResource(): StringResource = when (this) {
    PaymentErrorUi.INVALID_DETAILS -> Res.string.feature_payment_invalid_details
    PaymentErrorUi.LOAD -> Res.string.feature_payment_load_failed
    PaymentErrorUi.SAVE -> Res.string.feature_payment_save_failed
    PaymentErrorUi.SEND -> Res.string.feature_payment_send_failed
    PaymentErrorUi.SHARE -> Res.string.feature_payment_share_failed
    PaymentErrorUi.STALE -> Res.string.feature_payment_stale
}

internal fun Throwable.toPaymentErrorUi(fallback: PaymentErrorUi): PaymentErrorUi = when (this) {
    is PaymentUnavailableException -> PaymentErrorUi.STALE
    is InvalidPaymentDetailsException -> PaymentErrorUi.INVALID_DETAILS
    else -> fallback
}
