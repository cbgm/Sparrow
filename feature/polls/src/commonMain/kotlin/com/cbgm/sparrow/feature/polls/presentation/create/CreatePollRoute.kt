package com.cbgm.sparrow.feature.polls.presentation.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.selection.rememberMediaSelectionLauncher
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiEvent
import com.cbgm.sparrow.feature.polls.util.PollConstants
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreatePollRoute(
    viewModel: CreatePollViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaLauncher =
        rememberMediaSelectionLauncher(
            maxItems = PollConstants.MAX_MEDIA_ITEMS,
            maxImageDimension = MessageAttachmentPolicy.MAX_IMAGE_DIMENSION,
            maxImageBytes = MessageAttachmentPolicy.MAX_IMAGE_BYTES,
            maxVideoBytes = MessageAttachmentPolicy.MAX_VIDEO_BYTES,
            maxFileBytes = MessageAttachmentPolicy.MAX_FILE_BYTES,
            selectedMedia = uiState.media,
            onResult = { viewModel.onUiEvent(CreatePollUiEvent.MediaSelectionChanged(it)) },
            onFilePickerSessionStarted = {}
        )

    CreatePollScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        onAddMedia = { mediaLauncher.launch(MediaSourceUi.GALLERY) }
    )
}
