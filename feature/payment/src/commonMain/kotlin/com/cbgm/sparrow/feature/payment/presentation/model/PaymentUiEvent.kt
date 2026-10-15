package com.cbgm.sparrow.feature.payment.presentation.model

sealed interface PaymentUiEvent {
    data object Back : PaymentUiEvent

    data class HolderChanged(
        val value: String
    ) : PaymentUiEvent {
        override fun toString(): String = "HolderChanged(redacted)"
    }

    data class IbanChanged(
        val value: String
    ) : PaymentUiEvent {
        override fun toString(): String = "IbanChanged(redacted)"
    }

    data class RememberChanged(
        val value: Boolean
    ) : PaymentUiEvent

    data object ForgetDetails : PaymentUiEvent

    data object Review : PaymentUiEvent

    data object Edit : PaymentUiEvent

    data object Share : PaymentUiEvent

    data class ShareCompleted(
        val success: Boolean
    ) : PaymentUiEvent

    data object MarkSent : PaymentUiEvent

    data object ConfirmReceipt : PaymentUiEvent
}
