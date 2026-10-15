package com.cbgm.sparrow.feature.payment.device.mapper

import com.cbgm.sparrow.feature.media.device.QrCodeRenderRequest
import com.cbgm.sparrow.feature.media.device.QrErrorCorrection
import com.cbgm.sparrow.feature.payment.device.PaymentImageLabels
import com.cbgm.sparrow.feature.payment.device.PaymentImageRequest

internal fun PaymentImageRequest.toQrCodeRenderRequest(labels: PaymentImageLabels): QrCodeRenderRequest =
    QrCodeRenderRequest(
        content = epcPayload,
        size = 720,
        margin = 4,
        errorCorrection = QrErrorCorrection.MEDIUM,
        caption = listOf(
            labels.title,
            "${labels.recipient}:",
            accountHolder,
            "${labels.iban}: $iban",
            "${labels.amount}: $amount",
            "${labels.reference}:",
            reference
        )
    )
