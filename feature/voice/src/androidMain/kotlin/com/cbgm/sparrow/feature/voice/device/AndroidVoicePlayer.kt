package com.cbgm.sparrow.feature.voice.device

import android.media.MediaDataSource
import android.media.MediaPlayer

class AndroidVoicePlayer : VoicePlayer {
    private var player: MediaPlayer? = null
    private var seekInProgress = false
    private var resumeAfterSeek = false

    override fun prepare(bytes: ByteArray): Result<Unit> = runCatching {
        require(bytes.isNotEmpty()) { "Voice message must not be empty" }
        stop()
        player =
            MediaPlayer().apply {
                setDataSource(ByteArrayMediaDataSource(bytes))
                setOnSeekCompleteListener { mediaPlayer ->
                    seekInProgress = false
                    if (resumeAfterSeek) {
                        resumeAfterSeek = false
                        mediaPlayer.start()
                    }
                }
                prepare()
            }
    }

    override fun play(bytes: ByteArray): Result<Unit> =
        prepare(bytes).mapCatching {
            resume()
        }

    override fun pause() {
        resumeAfterSeek = false
        player?.takeIf(MediaPlayer::isPlaying)?.pause()
    }

    override fun resume() {
        val mediaPlayer = player ?: return
        if (seekInProgress) {
            resumeAfterSeek = true
        } else {
            mediaPlayer.start()
        }
    }

    override fun seekTo(positionMilliseconds: Long) {
        val mediaPlayer = player ?: return
        seekInProgress = true
        mediaPlayer.seekTo(
            positionMilliseconds.coerceAtLeast(0L),
            MediaPlayer.SEEK_CLOSEST
        )
    }

    override fun stop() {
        seekInProgress = false
        resumeAfterSeek = false
        player?.release()
        player = null
    }

    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override val currentPositionMilliseconds: Long
        get() = player?.currentPosition?.toLong() ?: 0L
}

private class ByteArrayMediaDataSource(
    private val bytes: ByteArray
) : MediaDataSource() {
    override fun readAt(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        size: Int
    ): Int {
        if (position >= bytes.size) return -1
        val count = minOf(size, bytes.size - position.toInt())
        bytes.copyInto(
            destination = buffer,
            destinationOffset = offset,
            startIndex = position.toInt(),
            endIndex = position.toInt() + count
        )
        return count
    }

    override fun getSize(): Long = bytes.size.toLong()

    override fun close() = Unit
}
