package com.cbgm.sparrow.core.messagepart.domain.model

object PollPolicy {
    const val MIN_OPTIONS = 2
    const val MAX_OPTIONS = 6
    const val MAX_QUESTION_LENGTH = 200
    const val MAX_DESCRIPTION_LENGTH = 500

    fun requireValid(poll: Poll) {
        require(poll.id.isNotBlank()) { "Poll ID must not be blank" }
        require(poll.question.isNotBlank() && poll.question.length <= MAX_QUESTION_LENGTH) { "Invalid poll question" }
        require((poll.description?.length ?: 0) <= MAX_DESCRIPTION_LENGTH) { "Poll description is too long" }
        require(poll.options.size in MIN_OPTIONS..MAX_OPTIONS) { "A poll requires two to six options" }
        require(poll.options.all { it.id.isNotBlank() && it.text.isNotBlank() }) { "Poll options must not be blank" }
        require(poll.options.map(PollOption::id).distinct().size == poll.options.size) { "Poll option IDs must be unique" }
        require(poll.images.none { it.id == poll.id }) { "Poll and image IDs must differ" }
        MessageAttachmentPolicy.requireValid(poll.images)
        require(poll.images.all { it.byteSize in 1L..MessageAttachmentPolicy.MAX_IMAGE_BYTES.toLong() }) {
            "Invalid poll image size"
        }
        MessageAttachmentPolicy.requireValidTotalPayloadBytes(poll.images.sumOf(Image::byteSize))
        require(poll.expiresAtEpochMilliseconds == null || poll.expiresAtEpochMilliseconds > 0L)
        require(poll.closedAtEpochMilliseconds == null || poll.closedAtEpochMilliseconds > 0L)
    }
}
