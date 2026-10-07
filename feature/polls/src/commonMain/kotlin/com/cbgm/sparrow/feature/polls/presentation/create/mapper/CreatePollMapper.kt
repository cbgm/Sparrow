package com.cbgm.sparrow.feature.polls.presentation.create.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.PollOption
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMessagePart
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiState

internal fun CreatePollUiState.toPoll(id: String): Poll =
    Poll(
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
