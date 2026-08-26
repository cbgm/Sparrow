package com.cbgm.sparrow.feature.media.device

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.scale
import androidx.photopicker.compose.EmbeddedPhotoPicker
import androidx.photopicker.compose.ExperimentalPhotoPickerComposeApi
import androidx.photopicker.compose.rememberEmbeddedPhotoPickerState
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.component.SparrowStaticScaffold
import com.cbgm.sparrow.core.ui.theme.rectangle
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.domain.model.GalleryMedia
import com.cbgm.sparrow.feature.media.domain.model.GalleryPickerConfig
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
actual fun rememberGalleryPickerLauncher(
    config: GalleryPickerConfig,
    selectedMedia: List<GalleryMedia>,
    strings: GalleryPickerStrings,
    onMediaSelected: (List<GalleryMedia>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): GalleryPickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentConfig = rememberUpdatedState(config)
    val currentSelectedMedia = rememberUpdatedState(selectedMedia)
    val currentOnMediaSelected = rememberUpdatedState(onMediaSelected)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    val currentOnError = rememberUpdatedState(onError)
    var isEmbeddedPickerVisible by remember { mutableStateOf(false) }

    fun handleSelection(
        uris: List<Uri>,
        emptyMeansDismissed: Boolean
    ) {
        if (uris.isEmpty()) {
            if (emptyMeansDismissed) {
                currentOnDismissed.value()
            } else {
                currentOnMediaSelected.value(emptyList())
            }
            return
        }

        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val existingBySource =
                        currentSelectedMedia.value
                            .mapNotNull { media -> media.sourceReference?.let { it to media } }
                            .toMap()

                    uris
                        .distinctBy(Uri::toString)
                        .take(currentConfig.value.maxItems)
                        .map { uri ->
                            existingBySource[uri.toString()]
                                ?: decodeGalleryMedia(context, uri, currentConfig.value)
                        }
                }
            }.onSuccess(currentOnMediaSelected.value)
                .onFailure { error ->
                    currentOnError.value(error.message ?: "Selected gallery media could not be read")
                }
        }
    }

    val singleLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            handleSelection(listOfNotNull(uri), emptyMeansDismissed = true)
        }

    val multipleLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(config.maxItems.coerceAtLeast(2))
        ) { uris ->
            handleSelection(uris, emptyMeansDismissed = true)
        }

    if (isEmbeddedPickerVisible && supportsEmbeddedPhotoPicker()) {
        EmbeddedGalleryPickerDialog(
            maxItems = config.maxItems,
            selectedMedia = selectedMedia,
            strings = strings,
            onSelectionComplete = { uris ->
                isEmbeddedPickerVisible = false
                handleSelection(uris, emptyMeansDismissed = false)
            },
            onDismissed = {
                isEmbeddedPickerVisible = false
                currentOnDismissed.value()
            },
            onError = { message ->
                isEmbeddedPickerVisible = false
                currentOnError.value(message)
            }
        )
    }

    return remember(singleLauncher, multipleLauncher, config.maxItems) {
        GalleryPickerLauncher(
            launch = {
                if (supportsEmbeddedPhotoPicker()) {
                    isEmbeddedPickerVisible = true
                } else {
                    val request =
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    if (config.maxItems == 1) {
                        singleLauncher.launch(request)
                    } else {
                        multipleLauncher.launch(request)
                    }
                }
            }
        )
    }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalPhotoPickerComposeApi::class)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = EMBEDDED_PICKER_EXTENSION)
@Composable
private fun EmbeddedGalleryPickerDialog(
    maxItems: Int,
    selectedMedia: List<GalleryMedia>,
    strings: GalleryPickerStrings,
    onSelectionComplete: (List<Uri>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
) {
    val initialUris =
        remember(selectedMedia) {
            selectedMedia
                .mapNotNull(GalleryMedia::sourceReference)
                .map(Uri::parse)
                .toSet()
        }
    var currentSelection by remember(initialUris) { mutableStateOf(initialUris) }

    val pickerState =
        rememberEmbeddedPhotoPickerState(
            initialExpandedValue = true,
            initialMediaSelection = initialUris,
            onSessionError = { error ->
                onError(error.message ?: "Gallery picker could not be opened")
            },
            onUriPermissionGranted = { uris ->
                currentSelection = currentSelection + uris
            },
            onUriPermissionRevoked = { uris ->
                currentSelection = currentSelection - uris.toSet()
            },
            onSelectionComplete = {
                onSelectionComplete(currentSelection.toList())
            }
        )

    val featureInfo =
        remember(maxItems) {
            EmbeddedPhotoPickerFeatureInfo.Builder()
                .setMaxSelectionLimit(maxItems)
                .setMimeTypes(mutableListOf("image/*", "video/*"))
                .setOrderedSelection(true)
                .build()
        }

    val closePicker = {
        if (currentSelection.isEmpty()) {
            onSelectionComplete(emptyList())
        } else {
            onDismissed()
        }
    }

    SparrowOverlayHost(
        visible = true,
        onDismissRequest = closePicker,
        modifier = Modifier.fillMaxSize(),
        horizontalPadding = MaterialTheme.spacing.zero,
        topPadding = MaterialTheme.spacing.zero,
        shape = MaterialTheme.shapes.rectangle
    ) { dismissOverlay ->
        SparrowStaticScaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = strings.title,
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (currentSelection.isEmpty()) {
                                    onSelectionComplete(emptyList())
                                } else {
                                    dismissOverlay()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = strings.closeContentDescription
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                )
            }
        ) { innerPadding ->
            EmbeddedPhotoPicker(
                state = pickerState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                embeddedPhotoPickerFeatureInfo = featureInfo
            )
        }
    }
}

private fun supportsEmbeddedPhotoPicker(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= EMBEDDED_PICKER_EXTENSION

private fun decodeGalleryMedia(
    context: Context,
    uri: Uri,
    config: GalleryPickerConfig
): GalleryMedia {
    val mimeType = context.contentResolver.getType(uri).orEmpty()
    return when {
        mimeType.startsWith("image/") -> decodeGalleryImage(context, uri, config)
        mimeType.startsWith("video/") -> decodeGalleryVideo(context, uri, mimeType, config)
        else -> error("Selected gallery item is not a supported image or video")
    }
}

private fun decodeGalleryImage(
    context: Context,
    uri: Uri,
    config: GalleryPickerConfig
): GalleryMedia {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap =
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val maxDimension = config.maxImageDimension
            if (maxDimension != null) {
                val sourceWidth = info.size.width
                val sourceHeight = info.size.height
                val longestSide = max(sourceWidth, sourceHeight)
                if (longestSide > maxDimension) {
                    val scale = maxDimension.toDouble() / longestSide.toDouble()
                    decoder.setTargetSize(
                        (sourceWidth * scale).roundToInt().coerceAtLeast(1),
                        (sourceHeight * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        }

    val bytes = encodeImage(bitmap, config.maxImageBytes)
    return GalleryMedia(
        type = MediaContentType.IMAGE,
        bytes = bytes,
        mimeType = "image/jpeg",
        sourceReference = uri.toString(),
        width = bitmap.width,
        height = bitmap.height
    )
}

private fun decodeGalleryVideo(
    context: Context,
    uri: Uri,
    mimeType: String,
    config: GalleryPickerConfig
): GalleryMedia {
    val maxVideoBytes = config.maxVideoBytes
    if (maxVideoBytes != null) {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            val knownLength = descriptor.length
            require(knownLength < 0L || knownLength <= maxVideoBytes) {
                "Selected video exceeds $maxVideoBytes bytes"
            }
        }
    }

    val bytes =
        context.contentResolver.openInputStream(uri)?.use { input ->
            ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (maxVideoBytes != null) {
                        require(total <= maxVideoBytes) {
                            "Selected video exceeds $maxVideoBytes bytes"
                        }
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } ?: error("Selected video could not be opened")

    var width: Int? = null
    var height: Int? = null
    var durationMilliseconds: Long? = null
    var previewBytes: ByteArray? = null
    runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            durationMilliseconds =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val previewTimeUs =
                durationMilliseconds
                    ?.takeIf { it > 0L }
                    ?.let { duration -> minOf(duration / 3L, VIDEO_PREVIEW_TIME_MILLISECONDS) * 1_000L }
                    ?: 0L
            previewBytes =
                retriever
                    .getFrameAtTime(previewTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let(::encodeVideoPreview)
        }
    }

    return GalleryMedia(
        type = MediaContentType.VIDEO,
        bytes = bytes,
        mimeType = mimeType.ifBlank { "video/mp4" },
        sourceReference = uri.toString(),
        previewBytes = previewBytes,
        width = width?.takeIf { it > 0 },
        height = height?.takeIf { it > 0 },
        durationMilliseconds = durationMilliseconds?.takeIf { it >= 0L }
    )
}

private fun encodeVideoPreview(bitmap: Bitmap): ByteArray {
    val longestSide = max(bitmap.width, bitmap.height)
    val previewBitmap =
        if (longestSide > VIDEO_PREVIEW_MAX_DIMENSION) {
            val scale = VIDEO_PREVIEW_MAX_DIMENSION.toDouble() / longestSide.toDouble()
            bitmap.scale(
                (bitmap.width * scale).roundToInt().coerceAtLeast(1),
                (bitmap.height * scale).roundToInt().coerceAtLeast(1)
            )
        } else {
            bitmap
        }

    return ByteArrayOutputStream().use { output ->
        check(previewBitmap.compress(Bitmap.CompressFormat.JPEG, VIDEO_PREVIEW_JPEG_QUALITY, output)) {
            "Selected video preview could not be encoded"
        }
        output.toByteArray()
    }
}

private fun encodeImage(
    bitmap: Bitmap,
    maxBytes: Int?
): ByteArray {
    for (quality in JPEG_QUALITIES) {
        val bytes =
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                    "Selected image could not be encoded"
                }
                output.toByteArray()
            }
        if (maxBytes == null || bytes.size <= maxBytes) return bytes
    }
    error("Selected image is too large after normalization")
}

private const val EMBEDDED_PICKER_EXTENSION = 15
private const val VIDEO_PREVIEW_MAX_DIMENSION = 320
private const val VIDEO_PREVIEW_TIME_MILLISECONDS = 1_000L
private const val VIDEO_PREVIEW_JPEG_QUALITY = 76
private val JPEG_QUALITIES = listOf(90, 84, 78, 72, 66, 60)
