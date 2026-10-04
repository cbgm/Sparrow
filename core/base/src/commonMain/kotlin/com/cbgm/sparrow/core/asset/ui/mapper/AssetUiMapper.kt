package com.cbgm.sparrow.core.asset.ui.mapper

import com.cbgm.sparrow.core.asset.domain.model.Asset
import com.cbgm.sparrow.core.asset.domain.model.Contact
import com.cbgm.sparrow.core.asset.domain.model.File
import com.cbgm.sparrow.core.asset.domain.model.Image
import com.cbgm.sparrow.core.asset.domain.model.Location
import com.cbgm.sparrow.core.asset.domain.model.Poll
import com.cbgm.sparrow.core.asset.domain.model.Text
import com.cbgm.sparrow.core.asset.domain.model.Video
import com.cbgm.sparrow.core.asset.domain.model.Voice
import com.cbgm.sparrow.core.asset.ui.model.AssetSourceUi
import com.cbgm.sparrow.core.asset.ui.model.AssetUi
import com.cbgm.sparrow.core.asset.ui.model.ContactUi
import com.cbgm.sparrow.core.asset.ui.model.FileUi
import com.cbgm.sparrow.core.asset.ui.model.ImageUi
import com.cbgm.sparrow.core.asset.ui.model.LocationUi
import com.cbgm.sparrow.core.asset.ui.model.PollOptionUi
import com.cbgm.sparrow.core.asset.ui.model.PollUi
import com.cbgm.sparrow.core.asset.ui.model.TextUi
import com.cbgm.sparrow.core.asset.ui.model.VideoUi
import com.cbgm.sparrow.core.asset.ui.model.VoiceUi

fun Asset.toAssetUi(source: AssetSourceUi = AssetSourceUi.Message): AssetUi =
    when (this) {
        is Text -> TextUi(id = id, text = text, source = source)
        is Image -> toImageUi(source)
        is Video ->
            VideoUi(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath,
                thumbnailFilePath = thumbnailFilePath,
                source = source
            )

        is File ->
            FileUi(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                localFilePath = localFilePath,
                source = source
            )

        is Voice ->
            VoiceUi(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = durationMilliseconds,
                source = source
            )

        is Location -> LocationUi(id = id, source = source)
        is Contact -> ContactUi(id = id, source = source)
        is Poll ->
            PollUi(
                id = id,
                question = question,
                description = description,
                options = options.map { PollOptionUi(id = it.id, text = it.text) },
                images = images.map { it.toImageUi(source) },
                allowMultipleSelection = allowMultipleSelection,
                allowVoteChange = allowVoteChange,
                isAnonymous = isAnonymous,
                expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                closedAtEpochMilliseconds = closedAtEpochMilliseconds,
                source = source
            )
    }

private fun Image.toImageUi(source: AssetSourceUi): ImageUi =
    ImageUi(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        width = width,
        height = height,
        fileName = fileName,
        localFilePath = localFilePath,
        thumbnailFilePath = thumbnailFilePath,
        source = source
    )
