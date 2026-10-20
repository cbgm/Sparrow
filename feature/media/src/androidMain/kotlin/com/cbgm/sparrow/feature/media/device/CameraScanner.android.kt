package com.cbgm.sparrow.feature.media.device

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.ImageFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.media.domain.model.CameraScanMode
import com.cbgm.sparrow.feature.media.domain.model.CameraScanResult
import com.cbgm.sparrow.feature.media.domain.model.DocumentCaptureMode
import com.cbgm.sparrow.feature.media.presentation.camera.CameraScannerControls
import com.cbgm.sparrow.feature.media.presentation.camera.DocumentCaptureControls
import com.cbgm.sparrow.feature.media.presentation.camera.ScanGuideShape
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_media_document_scanner_hint_automatic
import com.cbgm.sparrow.resources.feature_media_document_scanner_hint_detected
import com.cbgm.sparrow.resources.feature_media_document_scanner_hint_manual
import com.cbgm.sparrow.resources.feature_media_qr_scanner_hint
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Composable
actual fun CameraScanner(
    mode: CameraScanMode,
    onResult: (CameraScanResult) -> Unit,
    modifier: Modifier,
    hint: String?,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    when (mode) {
        CameraScanMode.QrCode ->
            QrCodeScanner(
                onResult = onResult,
                hint = hint ?: stringResource(Res.string.feature_media_qr_scanner_hint),
                onDismiss = onDismiss,
                modifier = modifier
            )
        is CameraScanMode.Document ->
            DocumentScanner(
                mode = mode,
                onResult = onResult,
                hint = hint,
                onDismiss = onDismiss,
                onError = onError,
                modifier = modifier
            )
    }
}

@Composable
actual fun rememberCameraScannerLauncher(
    onResult: (CameraScanResult) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): CameraScannerLauncher {
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val currentOnError by rememberUpdatedState(onError)
    var requestedMode by remember { mutableStateOf<CameraScanMode?>(null) }
    var openedMode by remember { mutableStateOf<CameraScanMode?>(null) }

    val requestPermission =
        rememberCameraPermissionRequest(
            onPermissionGranted = { openedMode = requestedMode },
            onPermissionDenied = {
                currentOnError("Camera permission was not granted")
                currentOnDismissed()
            }
        )

    openedMode?.let { mode ->
        val dismiss = {
            openedMode = null
            currentOnDismissed()
        }
        Dialog(
            onDismissRequest = dismiss,
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
        ) {
            CameraScanner(
                mode = mode,
                onResult = { result ->
                    openedMode = null
                    currentOnResult(result)
                },
                onDismiss = dismiss,
                onError = currentOnError,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(FunctionalColors.MediaBackground)
            )
        }
    }

    return remember(requestPermission) {
        CameraScannerLauncher(
            launch = { mode ->
                requestedMode = mode
                requestPermission()
            }
        )
    }
}

@Composable
private fun QrCodeScanner(
    onResult: (CameraScanResult) -> Unit,
    hint: String,
    onDismiss: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    val currentOnResult by rememberUpdatedState(onResult)
    val hasScanned = remember { AtomicBoolean(false) }
    val frameFailureReported = remember { AtomicBoolean(false) }
    val reader =
        remember {
            MultiFormatReader().apply {
                setHints(
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                        DecodeHintType.TRY_HARDER to true
                    )
                )
            }
        }

    Box(modifier = modifier) {
        CameraAnalysisPreview(
            tag = TAG,
            modifier = Modifier.fillMaxSize(),
            onCameraBound = { boundCamera -> camera = boundCamera },
            onFrame = { imageProxy ->
                val decoded = reader.decodeQrCode(imageProxy, frameFailureReported)
                if (decoded != null && hasScanned.compareAndSet(false, true)) {
                    ContextCompat.getMainExecutor(context).execute {
                        currentOnResult(CameraScanResult.QrCode(decoded))
                    }
                }
            }
        )

        CameraScannerControls(
            guideShape = ScanGuideShape.QR_CODE,
            hint = hint,
            isHighlighted = false,
            isFlashEnabled = flashEnabled,
            isFlashAvailable = camera?.cameraInfo?.hasFlashUnit() == true,
            onDismiss = onDismiss,
            onToggleFlash = {
                camera?.toggleTorch(flashEnabled)?.let { flashEnabled = it }
            }
        )
    }
}

@Composable
private fun DocumentScanner(
    mode: CameraScanMode.Document,
    onResult: (CameraScanResult) -> Unit,
    hint: String?,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)
    val previewView = rememberCameraPreviewView()
    val imageCapture =
        remember {
            ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
        }
    val detector = remember { DocumentFrameDetector() }
    val automatic = remember { AtomicBoolean(mode.captureMode == DocumentCaptureMode.AUTOMATIC) }
    val resetRequested = remember { AtomicBoolean(false) }
    val captureInFlight = remember { AtomicBoolean(false) }
    var captureMode by remember { mutableStateOf(mode.captureMode) }
    var isDocumentDetected by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }

    val failCapture: (String) -> Unit = { message ->
        isCapturing = false
        resetRequested.set(true)
        captureInFlight.set(false)
        currentOnError(message)
    }
    val capture = {
        isCapturing = true
        capturePhoto(
            imageCapture = imageCapture,
            previewView = previewView,
            outputFile = File.createTempFile("sparrow-document-", ".jpg", context.cacheDir),
            maxDimension = mode.maxImageDimension,
            maxBytes = mode.maxImageBytes,
            onCaptured = { captured -> currentOnResult(CameraScanResult.Document(captured)) },
            onError = failCapture
        )
    }
    val currentCapture by rememberUpdatedState(capture)

    val testImagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                captureInFlight.set(false)
                return@rememberLauncherForActivityResult
            }
            isCapturing = true
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        uri.toCapturedPhoto(context, mode.maxImageDimension, mode.maxImageBytes)
                    }
                }.onSuccess { captured -> currentOnResult(CameraScanResult.Document(captured)) }
                    .onFailure { failure ->
                        SparrowLog.error(TAG, "Test image could not be read", failure)
                        failCapture(failure.message ?: "Test image could not be read")
                    }
            }
        }

    Box(modifier = modifier) {
        CameraAnalysisPreview(
            tag = TAG,
            previewView = previewView,
            imageCapture = imageCapture,
            modifier = Modifier.fillMaxSize(),
            onCameraBound = { boundCamera -> camera = boundCamera },
            onCameraError = { failure ->
                currentOnError(failure.message ?: "Camera could not be opened")
            },
            onFrame = { imageProxy ->
                if (imageProxy.format != ImageFormat.YUV_420_888 || captureInFlight.get()) {
                    return@CameraAnalysisPreview
                }
                if (resetRequested.getAndSet(false)) detector.reset()
                val state = detector.analyzeFrame(imageProxy)
                val mainExecutor = ContextCompat.getMainExecutor(context)
                if (state.isDocumentDetected != isDocumentDetected) {
                    mainExecutor.execute { isDocumentDetected = state.isDocumentDetected }
                }
                val autoCapture = state.isReadyForCapture && automatic.get()
                if (autoCapture && captureInFlight.compareAndSet(false, true)) {
                    mainExecutor.execute { currentCapture() }
                }
            }
        )

        CameraScannerControls(
            guideShape = ScanGuideShape.DOCUMENT,
            hint = hint ?: documentHint(captureMode, isDocumentDetected),
            isHighlighted = isDocumentDetected && captureMode == DocumentCaptureMode.AUTOMATIC,
            isFlashEnabled = flashEnabled,
            isFlashAvailable = camera?.cameraInfo?.hasFlashUnit() == true,
            onDismiss = onDismiss,
            onToggleFlash = {
                camera?.toggleTorch(flashEnabled)?.let { flashEnabled = it }
            },
            capture =
                DocumentCaptureControls(
                    mode = captureMode,
                    isCapturing = isCapturing,
                    onModeSelected = { selected ->
                        captureMode = selected
                        automatic.set(selected == DocumentCaptureMode.AUTOMATIC)
                        resetRequested.set(true)
                    },
                    onCapture = { if (captureInFlight.compareAndSet(false, true)) capture() },
                    onPickTestImage =
                        if (context.isDebuggable()) {
                            {
                                if (captureInFlight.compareAndSet(false, true)) {
                                    testImagePicker.launch("image/*")
                                }
                            }
                        } else {
                            null
                        }
                )
        )
    }
}

@Composable
private fun documentHint(
    captureMode: DocumentCaptureMode,
    isDocumentDetected: Boolean
): String =
    stringResource(
        when {
            captureMode == DocumentCaptureMode.MANUAL -> Res.string.feature_media_document_scanner_hint_manual
            isDocumentDetected -> Res.string.feature_media_document_scanner_hint_detected
            else -> Res.string.feature_media_document_scanner_hint_automatic
        }
    )

private fun MultiFormatReader.decodeQrCode(
    imageProxy: ImageProxy,
    frameFailureReported: AtomicBoolean
): String? {
    if (imageProxy.format != ImageFormat.YUV_420_888) return null
    return try {
        val upright = imageProxy.uprightLuminance()
        val source =
            PlanarYUVLuminanceSource(
                upright.bytes,
                upright.width,
                upright.height,
                0,
                0,
                upright.width,
                upright.height,
                false
            )
        decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            .text
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    } catch (_: NotFoundException) {
        null
    } catch (failure: Exception) {
        // One report per camera session: a broken frame stream can fail 30 times/second.
        if (frameFailureReported.compareAndSet(false, true)) {
            SparrowLog.error(TAG, "QR frame could not be analyzed", failure)
        }
        null
    } finally {
        reset()
    }
}

private fun DocumentFrameDetector.analyzeFrame(imageProxy: ImageProxy): DocumentFrameState =
    runCatching {
        val frame = imageProxy.uprightLuminance()
        analyze(
            luminance = frame.bytes,
            width = frame.width,
            height = frame.height,
            timestampMilliseconds = imageProxy.imageInfo.timestamp / NANOS_PER_MILLISECOND
        )
    }.onFailure { failure ->
        SparrowLog.error(TAG, "Document frame could not be analyzed", failure)
    }.getOrDefault(DocumentFrameState.None)

private fun Camera.toggleTorch(enabled: Boolean): Boolean? =
    takeIf { it.cameraInfo.hasFlashUnit() }?.let {
        cameraControl.enableTorch(!enabled)
        !enabled
    }

private fun Context.isDebuggable(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

private const val TAG = "CameraScanner"
private const val NANOS_PER_MILLISECOND = 1_000_000L
