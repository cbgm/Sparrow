package com.cbgm.sparrow.feature.voice.device

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosVoicePlayer : VoicePlayer {
    private var player: AVAudioPlayer? = null

    override fun prepare(bytes: ByteArray): Result<Unit> = runCatching {
        require(bytes.isNotEmpty()) { "Voice message must not be empty" }
        stop()

        val audioPlayer = AVAudioPlayer(data = bytes.toNSData(), error = null)
        check(audioPlayer.prepareToPlay()) { "Voice message could not be prepared" }
        player = audioPlayer
    }

    override fun play(bytes: ByteArray): Result<Unit> =
        prepare(bytes).mapCatching {
            check(player?.play() == true) { "Voice message could not be played" }
        }

    override fun pause() {
        player?.pause()
    }

    override fun resume() {
        player?.play()
    }

    override fun seekTo(positionMilliseconds: Long) {
        player?.currentTime = positionMilliseconds.coerceAtLeast(0L) / 1_000.0
    }

    override fun stop() {
        player?.stop()
        player = null
    }

    override val isPlaying: Boolean
        get() = player?.playing == true

    override val currentPositionMilliseconds: Long
        get() = ((player?.currentTime ?: 0.0) * 1_000.0).toLong()
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    require(isNotEmpty()) { "Voice message must not be empty" }
    return usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = size.toULong()
        )
    }
}
