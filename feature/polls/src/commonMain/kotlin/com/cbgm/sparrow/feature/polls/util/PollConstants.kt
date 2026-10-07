package com.cbgm.sparrow.feature.polls.util

import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.core.messagepart.domain.model.PollPolicy

object PollConstants {
    const val MIN_OPTIONS = PollPolicy.MIN_OPTIONS
    const val MAX_OPTIONS = PollPolicy.MAX_OPTIONS
    const val MAX_MEDIA_ITEMS = MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE
    const val MAX_QUESTION_LENGTH = PollPolicy.MAX_QUESTION_LENGTH
    const val MAX_DESCRIPTION_LENGTH = PollPolicy.MAX_DESCRIPTION_LENGTH
    const val MAX_VOTER_PREVIEW = 3
    const val MAX_MESSAGE_MEDIA_PREVIEW = 3
}
