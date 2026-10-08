package com.cbgm.sparrow.feature.polls.presentation.create.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.PollOption
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMessagePart
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiState
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_EXPIRY_MINUTES
import com.cbgm.sparrow.feature.polls.util.PollConstants.MILLISECONDS_PER_MINUTE

internal fun CreatePollUiState.toPoll(
    id: String,
    nowEpochMilliseconds: Long
): Poll {
    val expiresAtEpochMilliseconds =
        if (expiryEnabled) {
            val minutes = requireNotNull(expiryMinutes.toLongOrNull())
            require(minutes in 1L..MAX_EXPIRY_MINUTES)
            nowEpochMilliseconds + minutes * MILLISECONDS_PER_MINUTE
        } else {
            null
        }

    return Poll(
        id = id,
        question = question.trim(),
        description = description.trim().takeIf(String::isNotBlank),
        options = options.map { PollOption(it.id, it.text.trim()) },
        images = media.map { it.toMessagePart() as Image },
        allowMultipleSelection = allowMultipleSelection,
        allowVoteChange = allowVoteChange,
        isAnonymous = isAnonymous,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds
    )
}
