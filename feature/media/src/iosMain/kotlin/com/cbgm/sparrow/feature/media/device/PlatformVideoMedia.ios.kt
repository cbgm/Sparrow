package com.cbgm.sparrow.feature.media.device

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitView
import com.cbgm.sparrow.core.ui.component.rememberSparrowFallbackPainter
import com.cbgm.sparrow.feature.media.device.MediaImage
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readValue
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIColor
import platform.UIKit.UIView

@Composable
internal actual fun VideoThumbnail(
    media: MediaItem,
    modifier: Modifier,
    contentScale: ContentScale
) {
    MediaImage(
        data = media.thumbnailBytes,
        localFilePath = null,
        cacheKey = "media-thumbnail:${media.id}",
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun VideoPlayer(
    media: MediaItem,
    isActive: Boolean,
    modifier: Modifier
) {
    val localFilePath = media.localFilePath
    if (localFilePath != null) {
        VideoPlayerContent(
            media = media,
            url = NSURL.fileURLWithPath(localFilePath),
            isActive = isActive,
            modifier = modifier
        )
        return
    }

    val bytes = media.bytes
    if (bytes == null) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val fileState by
        produceState<VideoFileState>(
            initialValue = VideoFileState.Loading,
            media.id,
            bytes,
            media.mimeType
        ) {
            value =
                withContext(Dispatchers.Default) {
                    createVideoUrl(media)
                        ?.let(VideoFileState::Ready)
                        ?: VideoFileState.Failed
                }
        }

    when (val state = fileState) {
        VideoFileState.Loading ->
            Box(modifier = modifier.fillMaxSize().background(Color.Black))

        VideoFileState.Failed ->
            Image(
                painter = rememberSparrowFallbackPainter(),
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

        is VideoFileState.Ready ->
            VideoPlayerContent(
                media = media,
                url = state.url,
                isActive = isActive,
                modifier = modifier
            )
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun VideoPlayerContent(
    media: MediaItem,
    url: NSURL,
    isActive: Boolean,
    modifier: Modifier
) {
    val player = remember(media.id, url) { AVPlayer(uRL = url) }

    UIKitView(
        factory = {
            VideoView(
                frame = CGRectZero.readValue(),
                player = player
            )
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            if (isActive) {
                view.play()
            } else {
                view.pause()
            }
        },
        onRelease = VideoView::release
    )
}

@OptIn(ExperimentalForeignApi::class)
private class VideoView(
    frame: CValue<CGRect>,
    private val player: AVPlayer
) : UIView(frame = frame) {
    private val playerLayer =
        AVPlayerLayer().apply {
            this.player = this@VideoView.player
            videoGravity = AVLayerVideoGravityResizeAspect
        }

    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        playerLayer.frame = bounds
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun release() {
        player.pause()
        playerLayer.player = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createVideoUrl(media: MediaItem): NSURL? {
    val bytes = media.bytes ?: return null
    if (bytes.isEmpty()) return null

    val fileName = "sparrow-${media.id}.${media.mimeType.defaultExtension()}"
    val url =
        NSFileManager.defaultManager.temporaryDirectory
            .URLByAppendingPathComponent(fileName)
            ?: return null

    val data =
        bytes.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = bytes.size.toULong()
            )
        }

    return url.takeIf { data.writeToURL(it, atomically = true) }
}

private sealed interface VideoFileState {
    data object Loading : VideoFileState

    data object Failed : VideoFileState

    data class Ready(
        val url: NSURL
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
