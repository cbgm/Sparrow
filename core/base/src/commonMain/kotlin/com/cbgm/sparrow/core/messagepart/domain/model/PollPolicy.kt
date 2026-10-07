package com.cbgm.sparrow.core.messagepart.domain.model

object PollPolicy {
    const val MIN_OPTIONS = 2
    const val MAX_OPTIONS = 6
    const val MAX_QUESTION_LENGTH = 200
    const val MAX_DESCRIPTION_LENGTH = 500
    const val LOCAL_VOTER_ID = "__local_poll_voter__"

    fun requireValid(poll: Poll) {
        require(poll.id.isNotBlank()) { "Poll ID must not be blank" }
        require(poll.question.isNotBlank() && poll.question.length <= MAX_QUESTION_LENGTH) { "Invalid poll question" }
        require((poll.description?.length ?: 0) <= MAX_DESCRIPTION_LENGTH) { "Poll description is too long" }
        require(poll.options.size in MIN_OPTIONS..MAX_OPTIONS) { "A poll requires two to six options" }
        require(poll.options.all { it.id.isNotBlank() && it.text.isNotBlank() }) { "Poll options must not be blank" }
        require(poll.options.map(PollOption::id).distinct().size == poll.options.size) { "Poll option IDs must be unique" }
        require(poll.options.flatMap(PollOption::voterIds).none(String::isBlank)) { "Poll voter IDs must not be blank" }
        require(poll.images.none { it.id == poll.id }) { "Poll and image IDs must differ" }
        MessageAttachmentPolicy.requireValid(poll.images)
        require(poll.images.all { it.byteSize in 1L..MessageAttachmentPolicy.MAX_IMAGE_BYTES.toLong() }) {
            "Invalid poll image size"
        }
        MessageAttachmentPolicy.requireValidTotalPayloadBytes(poll.images.sumOf(Image::byteSize))
        require(poll.expiresAtEpochMilliseconds == null || poll.expiresAtEpochMilliseconds > 0L)
        require(poll.closedAtEpochMilliseconds == null || poll.closedAtEpochMilliseconds > 0L)
    }

    fun vote(
        poll: Poll,
        voterId: String,
        selectedOptionIds: Set<String>,
        nowEpochMilliseconds: Long
    ): Poll {
        requireValid(poll)
        require(voterId.isNotBlank()) { "Poll voter ID must not be blank" }
        require(selectedOptionIds.isNotEmpty()) { "A poll vote requires at least one option" }
        require(poll.closedAtEpochMilliseconds == null) { "Poll is closed" }
        require(poll.expiresAtEpochMilliseconds?.let { it > nowEpochMilliseconds } != false) { "Poll is expired" }

        val optionIds = poll.options.mapTo(mutableSetOf(), PollOption::id)
        require(selectedOptionIds.all { it in optionIds }) { "Poll vote contains an unknown option" }
        require(poll.allowMultipleSelection || selectedOptionIds.size == 1) { "Poll allows only one option" }

        val existingOptionIds =
            poll.options
                .filter { option -> voterId in option.voterIds }
                .mapTo(linkedSetOf()) { option -> option.id }
        if (existingOptionIds == selectedOptionIds) return poll
        require(poll.allowVoteChange || existingOptionIds.isEmpty()) { "Poll vote cannot be changed" }

        return poll.copy(
            options =
                poll.options.map { option ->
                    option.copy(
                        voterIds =
                            if (option.id in selectedOptionIds) {
                                option.voterIds + voterId
                            } else {
                                option.voterIds - voterId
                            }
                    )
                }
        )
    }

    fun close(poll: Poll, closedAtEpochMilliseconds: Long): Poll {
        requireValid(poll)
        require(closedAtEpochMilliseconds > 0L) { "Poll close timestamp must be positive" }
        val currentClosedAt = poll.closedAtEpochMilliseconds
        if (currentClosedAt == closedAtEpochMilliseconds) return poll
        require(currentClosedAt == null) { "Poll is already closed" }
        return poll.copy(closedAtEpochMilliseconds = closedAtEpochMilliseconds)
    }
}
