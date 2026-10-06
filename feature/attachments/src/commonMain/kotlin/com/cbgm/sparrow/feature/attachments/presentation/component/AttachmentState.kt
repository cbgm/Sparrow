package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.messagepart.ui.model.ContactUi
import com.cbgm.sparrow.core.messagepart.ui.model.FileUi
import com.cbgm.sparrow.core.messagepart.ui.model.ImageUi
import com.cbgm.sparrow.core.messagepart.ui.model.LocationUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartSourceUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.core.messagepart.ui.model.VideoUi
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentSource
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.presentation.AttachmentViewModel
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun rememberAttachmentUiState(
    part: MessagePartUi,
    load: Boolean = true
): AttachmentUiState {
    val target = part.toAttachmentTarget()
    val viewModel =
        koinViewModel<AttachmentViewModel>(key = target.viewModelKey) {
            parametersOf(target)
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, load) {
        if (load) viewModel.load()
    }

    return uiState
}

private fun MessagePartUi.toAttachmentTarget(): AttachmentTarget =
    AttachmentTarget(
        id = id,
        type = when (this) {
            is ImageUi -> MessageAttachmentType.IMAGE
            is VideoUi -> MessageAttachmentType.VIDEO
            is FileUi -> MessageAttachmentType.FILE
            is LocationUi -> MessageAttachmentType.LOCATION
            is ContactUi -> MessageAttachmentType.CONTACT
            else -> error("Message part $id is not handled by attachment content loading")
        },
        source = source.toAttachmentSource()
    )

private val MessagePartUi.source: MessagePartSourceUi
    get() = when (this) {
        is ImageUi -> source
        is VideoUi -> source
        is FileUi -> source
        is LocationUi -> source
        is ContactUi -> source
        else -> error("Message part $id does not expose an attachment source")
    }

private fun MessagePartSourceUi.toAttachmentSource(): AttachmentSource =
    when (this) {
        MessagePartSourceUi.Message -> AttachmentSource.Message
        is MessagePartSourceUi.GroupPin -> AttachmentSource.GroupPin(groupId)
    }

private val AttachmentTarget.viewModelKey: String
    get() =
        when (val attachmentSource = source) {
            AttachmentSource.Message -> "attachment:message:${type.name}:$id"
            is AttachmentSource.GroupPin ->
                "attachment:group-pin:${attachmentSource.groupId}:${type.name}:$id"
        }
