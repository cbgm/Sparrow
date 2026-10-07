package com.cbgm.sparrow.feature.polls.presentation.message.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollMessageUiState
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollOptionUi
import com.cbgm.sparrow.feature.polls.presentation.model.PollVoterUi
import com.cbgm.sparrow.feature.polls.presentation.voters.model.PollVoterSectionUi
import com.cbgm.sparrow.feature.polls.presentation.voters.model.PollVotersUiState
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_MESSAGE_MEDIA_PREVIEW
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_VOTER_PREVIEW
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun PollUi.toPollMessageUiState(): PollMessageUiState {
    val media =
        images.map { image ->
            MediaItemUi(
                id = image.id,
                type = MediaTypeUi.IMAGE,
                mimeType = image.mimeType,
                localFilePath = image.localFilePath,
                thumbnailFilePath = image.thumbnailFilePath,
                width = image.width,
                height = image.height
            )
        }
    val mediaPreview = media.take(MAX_MESSAGE_MEDIA_PREVIEW)
    val submittedOptionIds =
        options
            .filter { option -> PollPolicy.LOCAL_VOTER_ID in option.voterIds }
            .mapTo(linkedSetOf()) { option -> option.id }
    val voterIds = options.flatMapTo(linkedSetOf()) { option -> option.voterIds }
    val totalVoters = voterIds.size
    val isExpired = expiresAtEpochMilliseconds?.let { it <= SystemClock.nowEpochMilliseconds() } == true
    val isClosed = closedAtEpochMilliseconds != null
    val canInteract = !isClosed && !isExpired && (allowVoteChange || submittedOptionIds.isEmpty())

    val mappedOptions =
        options.map { option ->
            val visibleVoters =
                if (isAnonymous) {
                    emptyList()
                } else {
                    option.voterIds
                        .filterNot { voterId -> voterId == PollPolicy.LOCAL_VOTER_ID }
                        .map { voterId -> PollVoterUi(id = voterId, displayName = voterId) }
                }
            val percentage =
                if (totalVoters == 0) {
                    0
                } else {
                    ((option.voterIds.size.toDouble() / totalVoters.toDouble()) * 100.0)
                        .toInt()
                        .coerceIn(0, 100)
                }
            PollOptionUi(
                id = option.id,
                text = option.text,
                voteCount = option.voterIds.size,
                voters = visibleVoters,
                voterPreview = visibleVoters.take(MAX_VOTER_PREVIEW),
                percentage = percentage,
                isSelected = option.id in submittedOptionIds
            )
        }

    val canShowVotes = !isAnonymous && mappedOptions.any { option -> option.voters.isNotEmpty() }
    val votersOverlay =
        if (canShowVotes) {
            PollVotersUiState(
                pollId = id,
                question = question,
                totalVoters = totalVoters,
                sections =
                    mappedOptions.map { option ->
                        PollVoterSectionUi(
                            optionId = option.id,
                            optionText = option.text,
                            voteCount = option.voteCount,
                            percentage = option.percentage,
                            voters = option.voters
                        )
                    }
            )
        } else {
            null
        }

    return PollMessageUiState(
        pollId = id,
        question = question,
        description = description,
        media = media,
        mediaPreview = mediaPreview,
        remainingMediaCount = (media.size - mediaPreview.size).coerceAtLeast(0),
        options = mappedOptions,
        totalVoters = totalVoters,
        submittedOptionIds = submittedOptionIds,
        allowMultipleSelection = allowMultipleSelection,
        allowVoteChange = allowVoteChange,
        isAnonymous = isAnonymous,
        isClosed = isClosed,
        isExpired = isExpired,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        expiryLabel =
            expiresAtEpochMilliseconds?.let {
                Instant.fromEpochMilliseconds(it)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .toString()
                    .replace('T', ' ')
            },
        canClose = canClose && !isClosed,
        canInteract = canInteract,
        canShowVotes = canShowVotes,
        votersOverlay = votersOverlay
    )
}
