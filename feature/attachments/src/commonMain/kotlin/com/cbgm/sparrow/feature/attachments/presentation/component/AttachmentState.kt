package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.messagepart.ui.model.ContactUi
import com.cbgm.sparrow.core.messagepart.ui.model.LocationUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.presentation.AttachmentViewModel
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun rememberLocalFileUiState(
    part: MessagePartUi,
    load: Boolean = true
): AttachmentUiState<String> {
    val viewModel = rememberAttachmentViewModel(part)
    val uiState by viewModel.localFileState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, load) {
        if (load) viewModel.loadLocalFile()
    }

    return uiState
}

@Composable
fun rememberLocationUiState(
    part: LocationUi,
    load: Boolean = true
): AttachmentUiState<CurrentLocation> {
    val viewModel = rememberAttachmentViewModel(part)
    val uiState by viewModel.locationState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, load) {
        if (load) viewModel.loadLocation()
    }

    return uiState
}

@Composable
fun rememberContactUiState(
    part: ContactUi,
    load: Boolean = true
): AttachmentUiState<SharedContact> {
    val viewModel = rememberAttachmentViewModel(part)
    val uiState by viewModel.contactState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, load) {
        if (load) viewModel.loadContact()
    }

    return uiState
}

@Composable
private fun rememberAttachmentViewModel(part: MessagePartUi): AttachmentViewModel =
    koinViewModel<AttachmentViewModel>(key = part.instanceKey) {
        parametersOf(part)
    }
