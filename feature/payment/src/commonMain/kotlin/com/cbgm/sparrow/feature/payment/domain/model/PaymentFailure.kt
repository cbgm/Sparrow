package com.cbgm.sparrow.feature.payment.domain.model

class PaymentUnavailableException : IllegalStateException("The selected payment is no longer available")

class InvalidPaymentDetailsException : IllegalArgumentException("Invalid payment details")
