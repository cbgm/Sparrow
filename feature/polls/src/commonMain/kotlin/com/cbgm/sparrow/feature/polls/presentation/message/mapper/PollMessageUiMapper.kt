package com.cbgm.sparrow.feature.polls.presentation.message.mapper

import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollMessageUiState
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollOptionUi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun PollUi.toPollMessageUiState(): PollMessageUiState =
    PollMessageUiState(
        pollId = id,
        question = question,
        description = description,
        media = images.map { image ->
            MediaItemUi(
                id = image.id,
                type = MediaTypeUi.IMAGE,
                mimeType = image.mimeType,
                localFilePath = image.localFilePath,
                thumbnailFilePath = image.thumbnailFilePath,
                width = image.width,
                height = image.height
            )
        },
        options = options.map { PollOptionUi(id = it.id, text = it.text, voteCount = 0) },
        allowMultipleSelection = allowMultipleSelection,
        allowVoteChange = allowVoteChange,
        isAnonymous = isAnonymous,
        isClosed = closedAtEpochMilliseconds != null,
        isExpired = expiresAtEpochMilliseconds?.let { it <= SystemClock.nowEpochMilliseconds() } == true,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        expiryLabel = expiresAtEpochMilliseconds?.let {
            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).toString().replace('T', ' ')
        },
        isVotingAvailable = false
    )
