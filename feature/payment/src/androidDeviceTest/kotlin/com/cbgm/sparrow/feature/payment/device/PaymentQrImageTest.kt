package com.cbgm.sparrow.feature.payment.device

import android.graphics.BitmapFactory
import com.cbgm.sparrow.feature.media.device.QrCodeRenderRequest
import com.cbgm.sparrow.feature.media.device.renderQrCode
import com.cbgm.sparrow.feature.payment.device.mapper.toQrCodeRenderRequest
import com.cbgm.sparrow.feature.payment.domain.SepaPaymentPolicy
import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.model.SepaPayment
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentQrImageTest {
    @Test fun sameRendererProducesPlainQrWithoutCaption() {
        val payload = "sparrow-identity-test"
        val png = renderQrCode(QrCodeRenderRequest(content = payload))
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(png, 0, png.size))
        try {
            assertEquals(800, bitmap.width)
            assertEquals(800, bitmap.height)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))
            assertEquals(payload, decoded.text)
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun sharedPaymentImageContainsDecodableExactTransferData() {
        val payment = SepaPayment(BankDetails("Jörg Müller", "DE89370400440532013000"), 1205L, "SPARROW-test")
        val payload = SepaPaymentPolicy.epcPayload(payment)
        val request = PaymentImageRequest(
            1,
            payload,
            payment.bankDetails.accountHolder,
            payment.bankDetails.iban,
            "12.05",
            payment.reference
        )
        val labels = PaymentImageLabels("SEPA transfer", "Recipient", "IBAN", "Amount (EUR)", "Reference")
        val png = renderQrCode(request.toQrCodeRenderRequest(labels))
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(png, 0, png.size))
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.TRY_HARDER to true))
            assertEquals(payload, decoded.text)
        } finally {
            bitmap.recycle()
        }
    }
}
