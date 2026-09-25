package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentSource
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.presentation.AttachmentViewModel
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun rememberAttachmentUiState(
    target: AttachmentTarget,
    load: Boolean = true
): AttachmentUiState {
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

private val AttachmentTarget.viewModelKey: String
    get() =
        when (val attachmentSource = source) {
            AttachmentSource.Message -> "attachment:message:${type.name}:$id"
            is AttachmentSource.GroupPin ->
                "attachment:group-pin:${attachmentSource.groupId}:${type.name}:$id"
        }
