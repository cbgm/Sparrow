package com.cbgm.sparrow.feature.media.device

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
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
import com.cbgm.sparrow.core.logging.SparrowLog
import java.util.concurrent.Executors

@Composable
internal fun rememberCameraPreviewView(): PreviewView {
    val context = LocalContext.current
    return remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
}

@Composable
internal fun CameraAnalysisPreview(
    tag: String,
    onFrame: (ImageProxy) -> Unit,
    modifier: Modifier = Modifier,
    previewView: PreviewView = rememberCameraPreviewView(),
    imageCapture: ImageCapture? = null,
    onCameraBound: (Camera) -> Unit = {},
    onCameraError: (Throwable) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame = rememberUpdatedState(onFrame)
    val currentOnCameraBound = rememberUpdatedState(onCameraBound)
    val currentOnCameraError = rememberUpdatedState(onCameraError)

    DisposableEffect(lifecycleOwner, previewView, imageCapture) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener =
            Runnable {
                runCatching {
                    val provider = providerFuture.get()
                    val preview =
                        Preview.Builder().build().also { cameraPreview ->
                            cameraPreview.surfaceProvider = previewView.surfaceProvider
                        }
                    val analysis =
                        ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imageAnalysis ->
                                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    imageProxy.use { currentOnFrame.value(it) }
                                }
                            }
                    val useCases = listOfNotNull<UseCase>(preview, analysis, imageCapture)
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        *useCases.toTypedArray()
                    )
                }.onSuccess { camera ->
                    currentOnCameraBound.value(camera)
                }.onFailure { failure ->
                    SparrowLog.error(tag, "Could not bind camera to lifecycle", failure)
                    currentOnCameraError.value(failure)
                }
            }

        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            if (providerFuture.isDone) {
                runCatching { providerFuture.get().unbindAll() }
            }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
