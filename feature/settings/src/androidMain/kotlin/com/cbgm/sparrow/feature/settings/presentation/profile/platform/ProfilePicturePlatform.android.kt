package com.cbgm.sparrow.feature.settings.presentation.profile.platform

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_profile_picture
import com.cbgm.sparrow.resources.feature_settings_profile_picture_choose_gallery
import com.cbgm.sparrow.resources.feature_settings_profile_picture_crop
import com.cbgm.sparrow.resources.feature_settings_profile_picture_crop_hint
import com.cbgm.sparrow.resources.feature_settings_profile_picture_take_photo
import com.cbgm.sparrow.resources.feature_settings_profile_picture_use
import org.jetbrains.compose.resources.stringResource
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
actual fun rememberProfilePictureEditorLauncher(
    onPictureSelected: (ByteArray) -> Unit
): () -> Unit {
    val context = LocalContext.current
    var showSourceChooser by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            selectedBitmap = decodeBitmap(context = context, uri = uri)
        }

    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showCamera = true
        }

    if (showSourceChooser) {
        ProfilePictureSourceDialog(
            onCamera = {
                showSourceChooser = false
                if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    showCamera = true
                } else {
                    cameraPermission.launch(Manifest.permission.CAMERA)
                }
            },
            onGallery = {
                showSourceChooser = false
                picker.launch("image/*")
            },
            onDismiss = { showSourceChooser = false }
        )
    }

    if (showCamera) {
        ProfilePictureCameraScreen(
            onPictureCaptured = { bitmap ->
                showCamera = false
                selectedBitmap = bitmap
            },
            onDismiss = { showCamera = false }
        )
    }

    selectedBitmap?.let { bitmap ->
        ProfilePictureCropDialog(
            bitmap = bitmap,
            onConfirm = { bytes ->
                selectedBitmap = null
                onPictureSelected(bytes)
            },
            onDismiss = { selectedBitmap = null }
        )
    }

    return remember {
        { showSourceChooser = true }
    }
}

@Composable
actual fun ProfilePictureImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier
) {
    val bitmap =
        remember(bytes) {
            android.graphics.BitmapFactory
                .decodeByteArray(bytes, 0, bytes.size)
                ?.asImageBitmap()
        }

    bitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

@Composable
private fun ProfilePictureSourceDialog(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_settings_profile_picture),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onCamera,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.feature_settings_profile_picture_take_photo))
                }

                TextButton(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.feature_settings_profile_picture_choose_gallery))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.base_cancel))
            }
        }
    )
}

@Composable
private fun ProfilePictureCameraScreen(
    onPictureCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val previewView =
        remember {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val listener =
            Runnable {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview =
                        Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    val capture =
                        ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                    val cameraSelector =
                        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                    imageCapture = capture
                }
            }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            imageCapture = null
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                    .background(Color.Black)
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            TextButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.base_cancel),
                    color = Color.White
                )
            }

            Button(
                enabled = imageCapture != null,
                onClick = {
                    capturePhoto(
                        imageCapture = imageCapture ?: return@Button,
                        previewView = previewView,
                        outputFile =
                            File.createTempFile(
                                "sparrow-profile-",
                                ".jpg",
                                context.cacheDir
                            ),
                        onCaptured = onPictureCaptured
                    )
                },
                shape = CircleShape,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                        .size(76.dp)
            ) {
                Text("")
            }
        }
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    previewView: PreviewView,
    outputFile: File,
    onCaptured: (Bitmap) -> Unit
) {
    previewView.display?.rotation?.let { imageCapture.targetRotation = it }

    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(outputFile).build(),
        ContextCompat.getMainExecutor(previewView.context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = decodeBitmap(outputFile)
                outputFile.delete()
                bitmap?.let(onCaptured)
            }

            override fun onError(exception: ImageCaptureException) {
                outputFile.delete()
            }
        }
    )
}

@Composable
private fun ProfilePictureCropDialog(
    bitmap: Bitmap,
    onConfirm: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var centerX by remember(bitmap) { mutableFloatStateOf(bitmap.width / 2f) }
    var centerY by remember(bitmap) { mutableFloatStateOf(bitmap.height / 2f) }
    var viewportSize by remember { mutableFloatStateOf(1f) }

    fun currentCropSize(): Float = min(bitmap.width, bitmap.height).toFloat() / zoom

    fun clampCenter() {
        val halfCrop = currentCropSize() / 2f
        centerX = centerX.coerceIn(halfCrop, bitmap.width - halfCrop)
        centerY = centerY.coerceIn(halfCrop, bitmap.height - halfCrop)
    }

    fun applyTransform(
        pan: Offset,
        zoomChange: Float
    ) {
        zoom = (zoom * zoomChange).coerceIn(MIN_CROP_ZOOM, MAX_CROP_ZOOM)
        val cropSize = currentCropSize()
        centerX -= pan.x / viewportSize * cropSize
        centerY -= pan.y / viewportSize * cropSize
        clampCenter()
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                    .background(Color.Black)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.base_cancel),
                    color = Color.White
                )
            }

            Text(
                text = stringResource(Res.string.feature_settings_profile_picture_crop),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp)
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CropViewport(
                    bitmap = bitmap.asImageBitmap(),
                    sourceWidth = bitmap.width,
                    sourceHeight = bitmap.height,
                    zoom = zoom,
                    centerX = centerX,
                    centerY = centerY,
                    onViewportSizeChanged = { viewportSize = it },
                    onTransform = { pan, zoomChange -> applyTransform(pan, zoomChange) }
                )

                Spacer(modifier = Modifier.size(20.dp))

                Text(
                    text = stringResource(Res.string.feature_settings_profile_picture_crop_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )

                Spacer(modifier = Modifier.size(12.dp))

                Slider(
                    value = zoom,
                    onValueChange = { newZoom ->
                        zoom = newZoom
                        clampCenter()
                    },
                    valueRange = MIN_CROP_ZOOM..MAX_CROP_ZOOM,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    onConfirm(
                        cropAndEncode(
                            bitmap = bitmap,
                            zoom = zoom,
                            centerX = centerX,
                            centerY = centerY
                        )
                    )
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Text(stringResource(Res.string.feature_settings_profile_picture_use))
            }
        }
    }
}

@Composable
private fun CropViewport(
    bitmap: ImageBitmap,
    sourceWidth: Int,
    sourceHeight: Int,
    zoom: Float,
    centerX: Float,
    centerY: Float,
    onViewportSizeChanged: (Float) -> Unit,
    onTransform: (pan: Offset, zoomChange: Float) -> Unit
) {
    val currentOnTransform by rememberUpdatedState(onTransform)
    val cropSize = min(sourceWidth, sourceHeight).toFloat() / zoom
    val left =
        (centerX - cropSize / 2f)
            .roundToInt()
            .coerceIn(0, (sourceWidth - cropSize.roundToInt()).coerceAtLeast(0))
    val top =
        (centerY - cropSize / 2f)
            .roundToInt()
            .coerceIn(0, (sourceHeight - cropSize.roundToInt()).coerceAtLeast(0))
    val sourceSize = cropSize.roundToInt().coerceAtLeast(1)

    Canvas(
        modifier =
            Modifier
                .size(320.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { size ->
                    onViewportSizeChanged(size.width.toFloat().coerceAtLeast(1f))
                }
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        currentOnTransform(pan, gestureZoom)
                    }
                }
    ) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(left, top),
            srcSize =
                IntSize(
                    width = min(sourceSize, sourceWidth - left),
                    height = min(sourceSize, sourceHeight - top)
                ),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
        )
    }
}

private fun cropAndEncode(
    bitmap: Bitmap,
    zoom: Float,
    centerX: Float,
    centerY: Float
): ByteArray {
    val cropSize = (min(bitmap.width, bitmap.height).toFloat() / zoom).roundToInt().coerceAtLeast(1)
    val halfCrop = cropSize / 2f
    val left =
        (centerX - halfCrop)
            .roundToInt()
            .coerceIn(0, bitmap.width - cropSize)
    val top =
        (centerY - halfCrop)
            .roundToInt()
            .coerceIn(0, bitmap.height - cropSize)

    val cropped = Bitmap.createBitmap(bitmap, left, top, cropSize, cropSize)
    val scaled =
        if (cropSize == PROFILE_PICTURE_SIZE) {
            cropped
        } else {
            Bitmap.createScaledBitmap(
                cropped,
                PROFILE_PICTURE_SIZE,
                PROFILE_PICTURE_SIZE,
                true
            )
        }

    return ByteArrayOutputStream().use { output ->
        check(scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
            "Profile picture could not be encoded"
        }
        output.toByteArray()
    }.also {
        if (scaled !== cropped) scaled.recycle()
        cropped.recycle()
    }
}

private fun decodeBitmap(
    context: android.content.Context,
    uri: android.net.Uri
): Bitmap? =
    runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        decodeBitmap(source)
    }.getOrNull()

private fun decodeBitmap(file: File): Bitmap? =
    runCatching {
        decodeBitmap(ImageDecoder.createSource(file))
    }.getOrNull()

private fun decodeBitmap(source: ImageDecoder.Source): Bitmap =
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val largestDimension = maxOf(info.size.width, info.size.height)
        if (largestDimension > MAX_DECODE_DIMENSION) {
            decoder.setTargetSampleSize(
                ceil(largestDimension.toDouble() / MAX_DECODE_DIMENSION).toInt()
            )
        }
    }

private const val MIN_CROP_ZOOM = 1f
private const val MAX_CROP_ZOOM = 6f
private const val PROFILE_PICTURE_SIZE = 512
private const val MAX_DECODE_DIMENSION = 2048
private const val JPEG_QUALITY = 88
