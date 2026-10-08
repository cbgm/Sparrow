package com.cbgm.sparrow.feature.chats.presentation.common.history.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePartSource
import com.cbgm.sparrow.core.messagepart.ui.mapper.toMessagePartUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi

internal fun List<MessagePart>.toMessagePartsUi(
    source: MessagePartSource = MessagePartSource.Message
): List<MessagePartUi> = map { part -> part.toMessagePartUi(source) }

internal fun MessageBubbleUi.toMessageAttachmentsUi(): List<MessagePartUi> =
    buildList {
        addAll(imageVideoParts)
        pollPart?.images?.let(::addAll)
        addAll(fileParts)
        locationPart?.let(::add)
        contactPart?.let(::add)
    }
