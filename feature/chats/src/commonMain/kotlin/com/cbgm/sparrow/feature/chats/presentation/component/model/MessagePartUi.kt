package com.cbgm.sparrow.feature.chats.presentation.component.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact

sealed interface MessagePartUi {
    data class ImageVideo(
        val id: String,
        val type: ImageVideoTypeUi,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        val localFilePath: String? = null
    ) : MessagePartUi

    data class File(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        val localFilePath: String? = null
    ) : MessagePartUi

    data class Location(
        val id: String,
        val location: CurrentLocation? = null
    ) : MessagePartUi

    data class Contact(
        val id: String,
        val contact: SharedContact? = null
    ) : MessagePartUi

    data class Text(
        val text: String,
        val isContentFailed: Boolean
    ) : MessagePartUi

    data class Voice(
        val durationMilliseconds: Long,
        val playbackPositionMilliseconds: Long = 0L,
        val isPlaying: Boolean = false,
        val waveform: List<Float> = emptyList()
    )
}

enum class ImageVideoTypeUi {
    IMAGE,
    VIDEO
}
