package com.cbgm.sparrow.feature.media.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitView
import com.cbgm.sparrow.feature.media.device.MediaImage
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIView

@Composable
internal actual fun VideoThumbnail(media: MediaItem, modifier: Modifier, contentScale: ContentScale) {
    MediaImage(
        data = null,
        localFilePath = media.thumbnailFilePath,
        cacheKey = "media-thumbnail:${media.id}",
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun VideoPlayer(media: MediaItem, isActive: Boolean, modifier: Modifier) {
    val path = media.localFilePath
    if (path != null) {
        VideoPlayerContent(media, NSURL.fileURLWithPath(path), isActive, modifier)
    } else {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
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
