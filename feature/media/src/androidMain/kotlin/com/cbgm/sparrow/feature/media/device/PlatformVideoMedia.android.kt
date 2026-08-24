package com.cbgm.sparrow.feature.media.device

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import com.cbgm.sparrow.core.ui.component.SparrowImage
import com.cbgm.sparrow.core.ui.component.rememberSparrowFallbackPainter
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal actual fun VideoThumbnail(
    media: MediaItem,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val fallback = rememberSparrowFallbackPainter()
    val context = LocalContext.current
    val bytes = media.bytes
    val request =
        remember(media.id, bytes, media.mimeType) {
            bytes?.let {
                ImageRequest.Builder(context)
                    .data(it)
                    .memoryCacheKey("media-thumbnail:${media.id}")
                    .videoFrameMillis(0)
                    .decoderFactory { result, options, _ ->
                        VideoFrameDecoder(result.source, options)
                    }
                    .build()
            }
        }

    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier.background(Color.Black),
        contentScale = contentScale,
        placeholder = null,
        error = fallback,
        fallback = null
    )
}

@Composable
internal actual fun VideoPlayer(
    media: MediaItem,
    isActive: Boolean,
    modifier: Modifier
) {
    val context = LocalContext.current
    val bytes = media.bytes
    if (bytes == null) {
        SparrowImage(
            model = media.thumbnailBytes,
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        return
    }

    val videoFileState by
        produceState<VideoFileState>(initialValue = VideoFileState.Loading, media.id, bytes) {
            value =
                runCatching {
                    withContext(Dispatchers.IO) {
                        val directory = File(context.cacheDir, "media-viewer").apply { mkdirs() }
                        val file = File(directory, "${media.id}.${media.mimeType.defaultExtension()}")
                        if (!file.exists() || file.length() != bytes.size.toLong()) {
                            file.writeBytes(bytes)
                        }
                        file.absolutePath
                    }
                }.fold(
                    onSuccess = VideoFileState::Ready,
                    onFailure = { VideoFileState.Failed }
                )
        }

    when (val state = videoFileState) {
        VideoFileState.Loading -> {
            Box(modifier = modifier.background(Color.Black))
            return
        }

        VideoFileState.Failed -> {
            SparrowImage(
                model = media.thumbnailBytes,
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            return
        }

        is VideoFileState.Ready -> {
            VideoView(
                media = media,
                path = state.path,
                isActive = isActive,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun VideoView(
    media: MediaItem,
    path: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    var videoView by remember(media.id) { mutableStateOf<VideoView?>(null) }
    var playbackFailed by remember(media.id, path) { mutableStateOf(false) }
    val currentIsActive by rememberUpdatedState(isActive)

    DisposableEffect(media.id, path) {
        onDispose {
            videoView?.stopPlayback()
            videoView = null
        }
    }

    if (playbackFailed) {
        SparrowImage(
            model = media.thumbnailBytes,
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        return
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { viewContext ->
            VideoView(viewContext).apply {
                val mediaController = MediaController(viewContext)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                tag = path
                setVideoPath(path)
                setOnPreparedListener { player ->
                    player.isLooping = false
                    if (currentIsActive) start()
                }
                setOnErrorListener { _, _, _ ->
                    playbackFailed = true
                    true
                }
                videoView = this
            }
        },
        update = { view ->
            if (view.tag != path) {
                view.tag = path
                view.setVideoPath(path)
            }
            if (!isActive && view.isPlaying) {
                view.pause()
            }
        }
    )
}

private sealed interface VideoFileState {
    data object Loading : VideoFileState

    data object Failed : VideoFileState

    data class Ready(
        val path: String
    ) : VideoFileState
}

private fun String.defaultExtension(): String =
    when (lowercase()) {
        "video/mp4" -> "mp4"
        "video/webm" -> "webm"
        "video/3gpp" -> "3gp"
        "video/quicktime" -> "mov"
        else -> substringAfterLast('/', "bin").takeIf(String::isNotBlank) ?: "bin"
    }
