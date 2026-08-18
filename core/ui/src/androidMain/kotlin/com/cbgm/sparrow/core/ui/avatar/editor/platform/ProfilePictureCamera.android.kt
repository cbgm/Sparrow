package com.cbgm.sparrow.core.ui.avatar.editor.platform

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
internal fun ProfilePictureCameraScreen(
    onPictureCaptured: (ByteArray) -> Unit,
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
                            it.surfaceProvider = previewView.surfaceProvider
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

        cameraProviderFuture.addListener(
            listener,
            ContextCompat.getMainExecutor(context)
        )

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
                    captureProfilePicture(
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
                        .size(Dimens.ProfilePictureCameraScreen.buttonSize)
            ) {
                Text("")
            }
        }
    }
}

private fun captureProfilePicture(
    imageCapture: ImageCapture,
    previewView: PreviewView,
    outputFile: File,
    onCaptured: (ByteArray) -> Unit
) {
    previewView.display?.rotation?.let { imageCapture.targetRotation = it }

    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(outputFile).build(),
        ContextCompat.getMainExecutor(previewView.context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(
                outputFileResults: ImageCapture.OutputFileResults
            ) {
                val bytes =
                    decodeProfilePictureBitmap(outputFile)
                        ?.let(::encodeProfilePictureSource)

                outputFile.delete()
                bytes?.let(onCaptured)
            }

            override fun onError(exception: ImageCaptureException) {
                outputFile.delete()
            }
        }
    )
}
