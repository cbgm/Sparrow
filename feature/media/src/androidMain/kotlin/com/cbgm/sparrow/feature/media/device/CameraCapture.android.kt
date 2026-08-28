package com.cbgm.sparrow.feature.media.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CameraLens
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia
import com.cbgm.sparrow.feature.media.presentation.camera.CameraControls
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
actual fun rememberCameraCaptureLauncher(
    config: CameraCaptureConfig,
    onCaptured: (CapturedMedia) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): CameraCaptureLauncher {
    val context = LocalContext.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val currentOnError by rememberUpdatedState(onError)
    var showCamera by remember(config) { mutableStateOf(false) }

    val requiredPermissions =
        remember(config.allowedTypes) {
            buildList {
                add(Manifest.permission.CAMERA)
                if (CameraCaptureType.VIDEO in config.allowedTypes) {
                    add(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = requiredPermissions.all { permission -> result[permission] == true }
            if (granted) {
                showCamera = true
            } else {
                currentOnError("Camera permission was not granted")
                currentOnDismissed()
            }
        }

    if (showCamera) {
        CameraCaptureDialog(
            config = config,
            onCaptured = { captured ->
                showCamera = false
                currentOnCaptured(captured)
            },
            onDismiss = {
                showCamera = false
                currentOnDismissed()
            },
            onError = currentOnError
        )
    }

    return remember(config, requiredPermissions, permissionLauncher, context) {
        CameraCaptureLauncher(
            launch = {
                val missing =
                    requiredPermissions.filter { permission ->
                        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                    }
                if (missing.isEmpty()) {
                    showCamera = true
                } else {
                    permissionLauncher.launch(missing.toTypedArray())
                }
            }
        )
    }
}

@Composable
private fun CameraCaptureDialog(
    config: CameraCaptureConfig,
    onCaptured: (CapturedMedia) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val currentOnError by rememberUpdatedState(onError)
    var selectedType by remember(config) { mutableStateOf(config.initialType) }
    var lens by remember(config) { mutableStateOf(config.initialLens) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var canSwitchCamera by remember { mutableStateOf(false) }
    var flashAvailable by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var recordingDurationMilliseconds by remember { mutableLongStateOf(0L) }

    val previewView =
        remember {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

    DisposableEffect(lifecycleOwner, previewView, lens, selectedType) {
        flashAvailable = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener =
            Runnable {
                runCatching {
                    val provider = providerFuture.get()
                    val preview =
                        Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                    val photoCapture =
                        ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                    val recorder = Recorder.Builder().build()
                    val movieCapture = VideoCapture.withOutput(recorder)
                    val selector = lens.resolveCameraSelector(provider)
                    val useCases =
                        buildList<androidx.camera.core.UseCase> {
                            add(preview)
                            when (selectedType) {
                                CameraCaptureType.PHOTO -> add(photoCapture)
                                CameraCaptureType.VIDEO -> add(movieCapture)
                            }
                        }

                    provider.unbindAll()
                    val boundCamera =
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            *useCases.toTypedArray()
                        )
                    camera = boundCamera
                    imageCapture = photoCapture.takeIf { selectedType == CameraCaptureType.PHOTO }
                    videoCapture = movieCapture.takeIf { selectedType == CameraCaptureType.VIDEO }
                    canSwitchCamera =
                        provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) &&
                        provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    flashAvailable = boundCamera.cameraInfo.hasFlashUnit()
                    if (flashEnabled && flashAvailable) {
                        boundCamera.cameraControl.enableTorch(true)
                    }
                }.onFailure { error ->
                    currentOnError(error.message ?: "Camera could not be opened")
                }
            }

        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            recording?.stop()
            recording = null
            imageCapture = null
            videoCapture = null
            camera = null
            if (providerFuture.isDone) {
                runCatching { providerFuture.get().unbindAll() }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (recording == null) onDismiss() },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(FunctionalColors.MediaBackground)
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(camera) {
                            detectTransformGestures { _, _, zoomChange, _ ->
                                val activeCamera = camera ?: return@detectTransformGestures
                                val zoomState = activeCamera.cameraInfo.zoomState.value ?: return@detectTransformGestures
                                val next =
                                    (zoomState.zoomRatio * zoomChange)
                                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                                activeCamera.cameraControl.setZoomRatio(next)
                            }
                        }.pointerInput(camera, previewView) {
                            detectTapGestures { offset ->
                                val activeCamera = camera ?: return@detectTapGestures
                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                activeCamera.cameraControl.startFocusAndMetering(
                                    FocusMeteringAction.Builder(point).build()
                                )
                            }
                        }
            )

            CameraControls(
                allowedTypes = config.allowedTypes,
                selectedType = selectedType,
                isRecording = recording != null,
                recordingDurationMilliseconds = recordingDurationMilliseconds,
                isFlashEnabled = flashEnabled,
                isFlashAvailable = flashAvailable,
                canSwitchCamera = canSwitchCamera,
                onDismiss = onDismiss,
                onCaptureTypeSelected = { selectedType = it },
                onCapture = {
                    when (selectedType) {
                        CameraCaptureType.PHOTO -> {
                            imageCapture?.let { capture ->
                                capturePhoto(
                                    imageCapture = capture,
                                    previewView = previewView,
                                    outputFile = File.createTempFile("sparrow-camera-", ".jpg", context.cacheDir),
                                    maxDimension = config.maxImageDimension,
                                    maxBytes = config.maxImageBytes,
                                    onCaptured = currentOnCaptured,
                                    onError = currentOnError
                                )
                            }
                        }

                        CameraCaptureType.VIDEO -> {
                            val activeRecording = recording
                            if (activeRecording != null) {
                                activeRecording.stop()
                            } else {
                                videoCapture?.let { capture ->
                                    val file = File.createTempFile("sparrow-camera-", ".mp4", context.cacheDir)
                                    recordingDurationMilliseconds = 0L
                                    startVideoRecording(
                                        context = context,
                                        videoCapture = capture,
                                        outputFile = file,
                                        maxBytes = config.maxVideoBytes,
                                        onStarted = { newRecording -> recording = newRecording },
                                        onStatus = { duration -> recordingDurationMilliseconds = duration },
                                        onFinalized = { captured, error ->
                                            recording = null
                                            recordingDurationMilliseconds = 0L
                                            if (captured != null) {
                                                currentOnCaptured(captured)
                                            } else if (error != null) {
                                                currentOnError(error)
                                            }
                                        },
                                        onError = currentOnError
                                    )
                                }
                            }
                        }
                    }
                },
                onSwitchCamera = {
                    lens =
                        when (lens) {
                            CameraLens.FRONT -> CameraLens.BACK
                            CameraLens.BACK -> CameraLens.FRONT
                        }
                    flashEnabled = false
                },
                onToggleFlash = {
                    val activeCamera = camera
                    if (activeCamera != null && activeCamera.cameraInfo.hasFlashUnit()) {
                        flashEnabled = !flashEnabled
                        activeCamera.cameraControl.enableTorch(flashEnabled)
                    }
                }
            )
        }
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    previewView: PreviewView,
    outputFile: File,
    maxDimension: Int?,
    maxBytes: Int?,
    onCaptured: (CapturedMedia) -> Unit,
    onError: (String) -> Unit
) {
    previewView.display?.rotation?.let { imageCapture.targetRotation = it }
    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(outputFile).build(),
        ContextCompat.getMainExecutor(previewView.context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                runCatching {
                    val bitmap = requireNotNull(
                        decodeProfilePictureBitmap(
                            file = outputFile,
                            maxDimension = maxDimension ?: DEFAULT_CAMERA_MAX_IMAGE_DIMENSION
                        )
                    ) {
                        "Captured photo could not be decoded"
                    }
                    try {
                        val bytes = bitmap.encodeCameraPhoto(maxBytes)
                        CapturedMedia(
                            type = CameraCaptureType.PHOTO,
                            bytes = bytes,
                            mimeType = "image/jpeg",
                            width = bitmap.width,
                            height = bitmap.height
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }.onSuccess(onCaptured)
                    .onFailure { error -> onError(error.message ?: "Photo could not be captured") }
                outputFile.delete()
            }

            override fun onError(exception: ImageCaptureException) {
                outputFile.delete()
                onError(exception.message ?: "Photo could not be captured")
            }
        }
    )
}

private fun android.graphics.Bitmap.encodeCameraPhoto(maxBytes: Int?): ByteArray {
    for (quality in CAMERA_JPEG_QUALITIES) {
        val bytes =
            ByteArrayOutputStream().use { output ->
                check(compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, output)) {
                    "Captured photo could not be encoded"
                }
                output.toByteArray()
            }
        if (maxBytes == null || bytes.size <= maxBytes) return bytes
    }
    error("Captured photo exceeds the configured size limit")
}

@SuppressLint("MissingPermission")
private fun startVideoRecording(
    context: android.content.Context,
    videoCapture: VideoCapture<Recorder>,
    outputFile: File,
    maxBytes: Long?,
    onStarted: (Recording) -> Unit,
    onStatus: (Long) -> Unit,
    onFinalized: (CapturedMedia?, String?) -> Unit,
    onError: (String) -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        outputFile.delete()
        onError("Microphone permission is required to record video")
        return
    }
    val builder = FileOutputOptions.Builder(outputFile)
    maxBytes?.let(builder::setFileSizeLimit)
    val pending =
        videoCapture.output
            .prepareRecording(context, builder.build())
            .withAudioEnabled()
    val recording =
        pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Status -> {
                    onStatus(event.recordingStats.recordedDurationNanos / 1_000_000L)
                }

                is VideoRecordEvent.Finalize -> {
                    val fileLimitReached =
                        event.error == VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
                    if (event.hasError() && !fileLimitReached) {
                        outputFile.delete()
                        onFinalized(null, "Video recording could not be completed")
                    } else {
                        runCatching { outputFile.toCapturedMedia() }
                            .onSuccess { captured ->
                                outputFile.delete()
                                onFinalized(captured, null)
                            }.onFailure { error ->
                                outputFile.delete()
                                onFinalized(null, error.message ?: "Video recording could not be read")
                            }
                    }
                }

                else -> Unit
            }
        }
    onStarted(recording)
}

private fun File.toCapturedMedia(): CapturedMedia {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(absolutePath)
        CapturedMedia(
            type = CameraCaptureType.VIDEO,
            bytes = readBytes(),
            mimeType = "video/mp4",
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
            durationMilliseconds = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        )
    } finally {
        retriever.release()
    }
}

private fun CameraLens.resolveCameraSelector(provider: ProcessCameraProvider): CameraSelector {
    val preferred = toCameraSelector()
    if (provider.hasCamera(preferred)) return preferred

    val fallback =
        when (this) {
            CameraLens.FRONT -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraLens.BACK -> CameraSelector.DEFAULT_FRONT_CAMERA
        }
    check(provider.hasCamera(fallback)) { "No camera is available" }
    return fallback
}

private fun CameraLens.toCameraSelector(): CameraSelector =
    when (this) {
        CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        CameraLens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
    }

private const val DEFAULT_CAMERA_MAX_IMAGE_DIMENSION = 4096
private val CAMERA_JPEG_QUALITIES = listOf(92, 88, 84, 80, 76, 72, 68, 64, 60)
