package com.cbgm.sparrow.feature.contactimport.device

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("UnsafeOptInUsageError")
@Composable
actual fun QrScanner(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnQrCodeScanned = rememberUpdatedState(newValue = onQrCodeScanned)

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val hasScanned = remember { AtomicBoolean(false) }

    val previewView =
        remember {
            PreviewView(context).apply {
                implementationMode =
                    PreviewView.ImplementationMode.COMPATIBLE

                scaleType =
                    PreviewView.ScaleType.FILL_CENTER
            }
        }

    DisposableEffect(
        lifecycleOwner,
        previewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val listener =
            Runnable {
                val cameraProvider = cameraProviderFuture.get()

                val preview =
                    Preview.Builder().build().also { cameraPreview ->
                        cameraPreview.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val reader =
                    MultiFormatReader().apply {
                        setHints(
                            mapOf(
                                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                                DecodeHintType.TRY_HARDER to true
                            )
                        )
                    }

                val imageAnalysis =
                    ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                cameraExecutor
                            ) { imageProxy ->
                                analyzeQrImage(
                                    imageProxy = imageProxy,
                                    reader = reader,
                                    hasScanned = hasScanned,
                                    onQrCodeScanned = { decodedValue ->

                                        ContextCompat
                                            .getMainExecutor(context)
                                            .execute { currentOnQrCodeScanned.value(decodedValue) }
                                    }
                                )
                            }
                        }

                try {
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                /*
                 * The UI remains visible. Camera errors can be
                 * surfaced through a dedicated callback later.
                 */
                }
            }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching {
                    cameraProviderFuture.get().unbindAll()
                }
            }

            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = {
            previewView
        },
        modifier = modifier
    )
}

private fun analyzeQrImage(
    imageProxy: ImageProxy,
    reader: MultiFormatReader,
    hasScanned: AtomicBoolean,
    onQrCodeScanned: (String) -> Unit
) {
    try {
        if (hasScanned.get()) {
            return
        }

        if (imageProxy.format != ImageFormat.YUV_420_888) {
            return
        }

        val luminanceData = imageProxy.copyLuminancePlane()

        val rotated =
            rotateLuminance(
                source = luminanceData,
                width = imageProxy.width,
                height = imageProxy.height,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees
            )

        val luminanceSource =
            PlanarYUVLuminanceSource(
                rotated.bytes,
                rotated.width,
                rotated.height,
                0,
                0,
                rotated.width,
                rotated.height,
                false
            )

        val bitmap = BinaryBitmap(HybridBinarizer(luminanceSource))

        val result = reader.decodeWithState(bitmap)

        val decodedValue =
            result.text
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return

        if (hasScanned.compareAndSet(false, true)) {
            onQrCodeScanned(decodedValue)
        }
    } catch (_: NotFoundException) {
        /*
         * No QR code exists in this frame.
         */
    } catch (_: Exception) {
        /*
         * Ignore malformed frames and continue scanning.
         */
    } finally {
        reader.reset()
        imageProxy.close()
    }
}

private fun ImageProxy.copyLuminancePlane(): ByteArray {
    val plane = planes.first()

    val buffer = plane.buffer

    val rowStride = plane.rowStride

    val pixelStride = plane.pixelStride

    val output = ByteArray(width * height)

    buffer.rewind()

    var outputIndex = 0

    for (row in 0 until height) {
        val rowStart = row * rowStride

        for (column in 0 until width) {
            val bufferIndex = rowStart + column * pixelStride

            if (bufferIndex < buffer.limit()) {
                output[outputIndex] = buffer.get(bufferIndex)
            }

            outputIndex += 1
        }
    }

    return output
}

private data class RotatedLuminance(
    val bytes: ByteArray,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RotatedLuminance

        if (width != other.width) return false
        if (height != other.height) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

private fun rotateLuminance(
    source: ByteArray,
    width: Int,
    height: Int,
    rotationDegrees: Int
): RotatedLuminance =
    when (rotationDegrees) {
        90 -> {
            val rotated = ByteArray(source.size)

            var destinationIndex = 0

            for (x in 0 until width) {
                for (y in height - 1 downTo 0) {
                    rotated[destinationIndex++] = source[y * width + x]
                }
            }

            RotatedLuminance(bytes = rotated, width = height, height = width)
        }

        180 -> {
            val rotated = ByteArray(source.size)

            source.indices.forEach { index ->
                rotated[source.lastIndex - index] = source[index]
            }

            RotatedLuminance(bytes = rotated, width = width, height = height)
        }

        270 -> {
            val rotated = ByteArray(source.size)

            var destinationIndex = 0

            for (x in width - 1 downTo 0) {
                for (y in 0 until height) {
                    rotated[destinationIndex++] = source[y * width + x]
                }
            }

            RotatedLuminance(bytes = rotated, width = height, height = width)
        }

        else -> {
            RotatedLuminance(bytes = source, width = width, height = height)
        }
    }
