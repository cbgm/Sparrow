package com.cbgm.sparrow.feature.chats.presentation.common.history.model

import com.cbgm.sparrow.core.messagepart.ui.model.ContactUi
import com.cbgm.sparrow.core.messagepart.ui.model.ExpenseBoardUi
import com.cbgm.sparrow.core.messagepart.ui.model.FileUi
import com.cbgm.sparrow.core.messagepart.ui.model.ImageVideoUi
import com.cbgm.sparrow.core.messagepart.ui.model.LocationUi
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.messagepart.ui.model.TextUi
import com.cbgm.sparrow.core.messagepart.ui.model.VoiceUi
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMessageUi
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

data class MessageBubbleUi(
    val id: String,
    val isMine: Boolean,
    val security: MessageSecurity,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus,
    val groupExtension: GroupMessageUi? = null,
    val canEdit: Boolean = false,
    val senderName: String? = null,
    val senderIsInContacts: Boolean = true,
    val deliveryProgress: DeliveryProgressUi = DeliveryProgressUi(),
    val safetyWarning: MessageSafetyWarningUi? = null,
    val reply: MessageReplyUi? = null,
    val reactions: List<MessageReactionUi> = emptyList(),
    val fileParts: List<FileUi> = emptyList(),
    val imageVideoParts: List<ImageVideoUi> = emptyList(),
    val locationPart: LocationUi? = null,
    val contactPart: ContactUi? = null,
    val voicePart: VoiceUi? = null,
    val textPart: TextUi? = null,
    val pollPart: PollUi? = null,
    val expenseBoardPart: ExpenseBoardUi? = null
)

data class DeliveryProgressUi(
    val recipientCount: Int = 0,
    val deliveredCount: Int = 0,
    val readCount: Int = 0
)

data class MessageReplyUi(
    val messageId: String,
    val isMine: Boolean? = null,
    val senderName: String? = null,
    val previewText: String? = null
)

data class MessageReactionUi(
    val emoji: String,
    val count: Int,
    val reactedByMe: Boolean
)
