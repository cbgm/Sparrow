package com.cbgm.sparrow.core.asset.data.mapper

import com.cbgm.sparrow.core.asset.data.model.AssetDto
import com.cbgm.sparrow.core.asset.data.model.ContactDto
import com.cbgm.sparrow.core.asset.data.model.FileDto
import com.cbgm.sparrow.core.asset.data.model.ImageDto
import com.cbgm.sparrow.core.asset.data.model.LocationDto
import com.cbgm.sparrow.core.asset.data.model.PollDto
import com.cbgm.sparrow.core.asset.data.model.TextDto
import com.cbgm.sparrow.core.asset.data.model.VideoDto
import com.cbgm.sparrow.core.asset.data.model.VoiceDto
import com.cbgm.sparrow.core.asset.domain.model.Asset
import com.cbgm.sparrow.core.asset.domain.model.Contact
import com.cbgm.sparrow.core.asset.domain.model.File
import com.cbgm.sparrow.core.asset.domain.model.Image
import com.cbgm.sparrow.core.asset.domain.model.Location
import com.cbgm.sparrow.core.asset.domain.model.Poll
import com.cbgm.sparrow.core.asset.domain.model.PollOption
import com.cbgm.sparrow.core.asset.domain.model.Text
import com.cbgm.sparrow.core.asset.domain.model.Video
import com.cbgm.sparrow.core.asset.domain.model.Voice

fun AssetDto.toAsset(): Asset =
    when (this) {
        is TextDto ->
            Text(
                id = id,
                text = text
            )

        is ImageDto -> toImage()

        is VideoDto ->
            Video(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath,
                thumbnailFilePath = thumbnailFilePath
            )

        is FileDto ->
            File(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                localFilePath = localFilePath
            )

        is VoiceDto ->
            Voice(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = durationMilliseconds
            )

        is LocationDto -> Location(id = id)
        is ContactDto -> Contact(id = id)
        is PollDto ->
            Poll(
                id = id,
                question = question,
                description = description,
                options = options.map { PollOption(id = it.id, text = it.text) },
                images = images.map(ImageDto::toImage),
                allowMultipleSelection = allowMultipleSelection,
                allowVoteChange = allowVoteChange,
                isAnonymous = isAnonymous,
                expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                closedAtEpochMilliseconds = closedAtEpochMilliseconds
            )
    }

private fun ImageDto.toImage(): Image =
    Image(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        width = width,
        height = height,
        fileName = fileName,
        localFilePath = localFilePath,
        thumbnailFilePath = thumbnailFilePath
    )
