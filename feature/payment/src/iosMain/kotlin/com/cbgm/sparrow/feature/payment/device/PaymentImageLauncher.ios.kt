@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.cbgm.sparrow.feature.payment.device

import com.cbgm.sparrow.feature.media.device.renderQrCode
import com.cbgm.sparrow.feature.payment.device.mapper.toQrCodeRenderRequest
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_payment_export_title
import com.cbgm.sparrow.resources.feature_payment_export_recipient
import com.cbgm.sparrow.resources.feature_payment_iban
import com.cbgm.sparrow.resources.feature_payment_amount
import com.cbgm.sparrow.resources.feature_payment_reference_label
import org.jetbrains.compose.resources.stringResource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@Composable
actual fun PaymentImageLauncher(request: PaymentImageRequest?, onComplete: (Boolean) -> Unit) {
    val labels = PaymentImageLabels(
        title = stringResource(Res.string.feature_payment_export_title),
        recipient = stringResource(Res.string.feature_payment_export_recipient),
        iban = stringResource(Res.string.feature_payment_iban),
        amount = stringResource(Res.string.feature_payment_amount),
        reference = stringResource(Res.string.feature_payment_reference_label)
    )
    val currentOnComplete by rememberUpdatedState(onComplete)
    var handledId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(request?.requestId) {
        val value = request ?: return@LaunchedEffect
        if (handledId == value.requestId) return@LaunchedEffect
        handledId = value.requestId
        try {
            val presenter = topViewController() ?: error("No share presenter")
            val bytes = renderQrCode(value.toQrCodeRenderRequest(labels))
            val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
            val image = requireNotNull(UIImage.imageWithData(data))
            val controller = UIActivityViewController(listOf(image), null)
            controller.completionWithItemsHandler = { _, _, _, error -> currentOnComplete(error == null) }
            controller.popoverPresentationController?.apply {
                sourceView = presenter.view
                sourceRect = CGRectMake(0.0, 0.0, 1.0, 1.0)
            }
            presenter.presentViewController(controller, animated = true, completion = null)
        } catch (_: Exception) { currentOnComplete(false) }
    }
}

private fun topViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
    var current = scenes.flatMap { it.windows.filterIsInstance<UIWindow>() }
        .firstOrNull { it.isKeyWindow() }?.rootViewController
    while (current?.presentedViewController != null) current = current.presentedViewController
    return current
}
