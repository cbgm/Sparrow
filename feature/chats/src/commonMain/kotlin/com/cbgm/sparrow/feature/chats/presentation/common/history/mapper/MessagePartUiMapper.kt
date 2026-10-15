package com.cbgm.sparrow.feature.chats.presentation.common.history.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePartSource
import com.cbgm.sparrow.core.messagepart.ui.mapper.toMessagePartUi
import com.cbgm.sparrow.core.messagepart.ui.model.ContactUi
import com.cbgm.sparrow.core.messagepart.ui.model.ExpenseBoardClosedUi
import com.cbgm.sparrow.core.messagepart.ui.model.ExpenseBoardUi
import com.cbgm.sparrow.core.messagepart.ui.model.ExpenseUi
import com.cbgm.sparrow.core.messagepart.ui.model.FileUi
import com.cbgm.sparrow.core.messagepart.ui.model.ImageVideoUi
import com.cbgm.sparrow.core.messagepart.ui.model.LocationUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.core.messagepart.ui.model.PaymentTransferUi
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.messagepart.ui.model.TextUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi

internal fun List<MessagePart>.toMessagePartsUi(
    source: MessagePartSource = MessagePartSource.Message
): List<MessagePartUi> = map { part -> part.toMessagePartUi(source) }

internal fun List<MessagePartUi>.standalonePartOrNull(): MessagePartUi? =
    firstOrNull { part -> part !is TextUi && part !is ImageVideoUi && part !is FileUi }

internal fun List<MessagePartUi>.textPartOrNull(): TextUi? = filterIsInstance<TextUi>().firstOrNull()

internal fun MessageBubbleUi.textOrNull(): String? = parts.textPartOrNull()?.text

internal fun MessageBubbleUi.toMessageAttachmentsUi(): List<MessagePartUi> =
    buildList {
        addAll(parts.filterIsInstance<ImageVideoUi>())
        parts.filterIsInstance<PollUi>().forEach { poll -> addAll(poll.images) }
        addAll(parts.filterIsInstance<FileUi>())
        addAll(parts.filterIsInstance<LocationUi>())
        addAll(parts.filterIsInstance<ContactUi>())
    }

internal fun MessageBubbleUi.expenseActivityPartOrNull(): MessagePartUi? =
    parts.firstOrNull { part -> part is PaymentTransferUi || part is ExpenseBoardUi || part is ExpenseBoardClosedUi || part is ExpenseUi }

internal val MessageBubbleUi.activeExpenseBoard: ExpenseBoardUi?
    get() = parts.filterIsInstance<ExpenseBoardUi>().firstOrNull { it.closedAtEpochMilliseconds == null }
