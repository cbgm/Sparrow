package com.cbgm.sparrow.feature.payment.device

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.cbgm.sparrow.feature.media.device.renderQrCode
import com.cbgm.sparrow.feature.payment.device.mapper.toQrCodeRenderRequest
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_payment_amount
import com.cbgm.sparrow.resources.feature_payment_export_recipient
import com.cbgm.sparrow.resources.feature_payment_export_title
import com.cbgm.sparrow.resources.feature_payment_iban
import com.cbgm.sparrow.resources.feature_payment_reference_label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.UUID

@Composable
actual fun PaymentImageLauncher(request: PaymentImageRequest?, onComplete: (Boolean) -> Unit) {
    val labels = PaymentImageLabels(
        title = stringResource(Res.string.feature_payment_export_title),
        recipient = stringResource(Res.string.feature_payment_export_recipient),
        iban = stringResource(Res.string.feature_payment_iban),
        amount = stringResource(Res.string.feature_payment_amount),
        reference = stringResource(Res.string.feature_payment_reference_label)
    )
    val context = LocalContext.current.applicationContext
    val currentOnComplete by rememberUpdatedState(onComplete)
    var handledId by remember { mutableStateOf<Int?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // This only means the sharing activity returned, never that a payment succeeded.
        currentOnComplete(true)
    }
    LaunchedEffect(request?.requestId) {
        val value = request ?: return@LaunchedEffect
        if (handledId == value.requestId) return@LaunchedEffect
        handledId = value.requestId
        var sharedFile: File? = null
        var sharedUri: Uri? = null
        try {
            val file = withContext(Dispatchers.IO) {
                val directory = File(context.cacheDir, "payment-share").apply { mkdirs() }
                directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 300_000L }
                    ?.forEach { it.delete() }
                File(directory, "payment-${UUID.randomUUID()}.png").also { output ->
                    sharedFile = output
                    val png = renderQrCode(value.toQrCodeRenderRequest(labels))
                    output.outputStream().use { it.write(png) }
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.payment.files", file)
            sharedUri = uri
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(labels.title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // Keep enough time for the target to import; cache is excluded from backups.
            Handler(Looper.getMainLooper()).postDelayed({
                context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                file.delete()
            }, 300_000L)
            launcher.launch(Intent.createChooser(intent, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (error: kotlinx.coroutines.CancellationException) {
            sharedUri?.let { context.revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            sharedFile?.delete()
            throw error
        } catch (_: Exception) {
            sharedUri?.let { context.revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            sharedFile?.delete()
            currentOnComplete(false)
        }
    }
}
