package com.cbgm.sparrow.feature.media.presentation.filepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSearchField
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FileBrowserEntryKind
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FileBrowserEntryUi
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerBreadcrumbUi
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSortMode
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerUiEvent
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_media_file_picker_add_files
import com.cbgm.sparrow.resources.feature_media_file_picker_already_selected
import com.cbgm.sparrow.resources.feature_media_file_picker_empty_folder
import com.cbgm.sparrow.resources.feature_media_file_picker_file_access
import com.cbgm.sparrow.resources.feature_media_file_picker_file_access_body
import com.cbgm.sparrow.resources.feature_media_file_picker_search
import com.cbgm.sparrow.resources.feature_media_file_picker_selected
import com.cbgm.sparrow.resources.feature_media_file_picker_sort
import com.cbgm.sparrow.resources.feature_media_file_picker_sort_name
import com.cbgm.sparrow.resources.feature_media_file_picker_sort_size
import com.cbgm.sparrow.resources.feature_media_file_picker_sort_type
import com.cbgm.sparrow.resources.feature_media_file_picker_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun FilePickerScreen(
    uiState: FilePickerUiState,
    onUiEvent: (FilePickerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { color ->
            FilePickerTopBar(
                containerColor = color,
                uiState = uiState,
                onUiEvent = onUiEvent
            )
        },
        bottomBar = { color ->
            FilePickerBottomBar(
                containerColor = color,
                uiState = uiState,
                onUiEvent = onUiEvent
            )
        }
    ) { innerPadding, listState ->
        when {
            uiState.requiresFileAccess ->
                FileAccessRequired(
                    onUiEvent = onUiEvent,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )

            uiState.isLoading && uiState.entries.isEmpty() ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            else ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding
                ) {
                    item(key = "breadcrumbs") {
                        Breadcrumbs(
                            breadcrumbs = uiState.breadcrumbs,
                            onUiEvent = onUiEvent
                        )
                    }

                    item(key = "search") {
                        FileSearchField(
                            value = uiState.searchQuery,
                            onUiEvent = onUiEvent
                        )
                    }

                    uiState.errorMessage?.let { message ->
                        item(key = "error") { ErrorMessage(message) }
                    }

                    if (!uiState.isLoading && uiState.entries.isEmpty()) {
                        item(key = "empty") { EmptyFolder() }
                    } else {
                        items(uiState.entries, key = FileBrowserEntryUi::reference) { entry ->
                            FileEntryRow(
                                entry = entry,
                                selected = entry.reference in uiState.selectedReferences,
                                onUiEvent = onUiEvent
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun FileAccessRequired(
    onUiEvent: (FilePickerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.spacing.large + MaterialTheme.spacing.large)
        )
        Text(
            text = stringResource(Res.string.feature_media_file_picker_file_access_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium)
        )
        SparrowApprovalButton(
            onClick = { onUiEvent(FilePickerUiEvent.GrantFileAccessClicked) },
            text = stringResource(Res.string.feature_media_file_picker_file_access),
            fillMaxWidth = false
        )
    }
}

@Composable
private fun Breadcrumbs(
    breadcrumbs: List<FilePickerBreadcrumbUi>,
    onUiEvent: (FilePickerUiEvent) -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, breadcrumb ->
            Text(
                text = breadcrumb.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (index == breadcrumbs.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                color =
                    if (index == breadcrumbs.lastIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                modifier =
                    if (index == breadcrumbs.lastIndex) {
                        Modifier
                    } else {
                        Modifier.clickable { onUiEvent(FilePickerUiEvent.BreadcrumbClicked(breadcrumb.reference)) }
                    }
            )
            if (index < breadcrumbs.lastIndex) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
                )
            }
        }
    }
}

@Composable
private fun FileSearchField(
    value: String,
    onUiEvent: (FilePickerUiEvent) -> Unit
) {
    SparrowSearchField(
        searchQuery = value,
        onSearchQueryChanged = { query -> onUiEvent(FilePickerUiEvent.SearchChanged(query)) },
        onClear = { onUiEvent(FilePickerUiEvent.SearchCleared) },
        placeholder = stringResource(Res.string.feature_media_file_picker_search),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small)
    )
}

@Composable
private fun FileEntryRow(
    entry: FileBrowserEntryUi,
    selected: Boolean,
    onUiEvent: (FilePickerUiEvent) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = entry.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            when {
                entry.isBlocked -> Text(stringResource(Res.string.feature_media_file_picker_already_selected))
                entry.isDirectory -> Unit
                else -> FileMetadata(entry)
            }
        },
        leadingContent = {
            Icon(
                imageVector = entry.kind.icon(),
                contentDescription = null
            )
        },
        trailingContent = {
            if (entry.isDirectory) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            } else {
                Icon(
                    imageVector =
                        if (selected || entry.isBlocked) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                    contentDescription = null,
                    tint =
                        if (selected || entry.isBlocked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                )
            }
        },
        modifier =
            Modifier.clickable(
                enabled = entry.isDirectory || !entry.isBlocked,
                onClick = { onUiEvent(FilePickerUiEvent.EntryClicked(entry.reference)) }
            )
    )
}

@Composable
private fun FileMetadata(entry: FileBrowserEntryUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        entry.typeText?.let { type -> Text(type) }
        if (entry.typeText != null && entry.sizeText != null) Text(" · ")
        entry.sizeText?.let { size -> Text(size) }
    }
}

@Composable
private fun EmptyFolder() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Folder, contentDescription = null)
        Text(
            text = stringResource(Res.string.feature_media_file_picker_empty_folder),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = MaterialTheme.spacing.base)
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilePickerTopBar(
    containerColor: Color,
    uiState: FilePickerUiState,
    onUiEvent: (FilePickerUiEvent) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text =
                    uiState.currentDirectoryName.ifBlank {
                        stringResource(Res.string.feature_media_file_picker_title)
                    },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = { onUiEvent(FilePickerUiEvent.BackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.base_back)
                )
            }
        },
        actions = {
            IconButton(onClick = { onUiEvent(FilePickerUiEvent.CloseClicked) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.base_close)
                )
            }
            if (!uiState.requiresFileAccess) {
                IconButton(onClick = { onUiEvent(FilePickerUiEvent.SortDirectionToggled) }) {
                    Icon(
                        imageVector =
                            if (uiState.sortAscending) {
                                Icons.Default.ArrowUpward
                            } else {
                                Icons.Default.ArrowDownward
                            },
                        contentDescription = stringResource(Res.string.feature_media_file_picker_sort)
                    )
                }
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(Res.string.feature_media_file_picker_sort)
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortItem(
                            title = stringResource(Res.string.feature_media_file_picker_sort_name),
                            selected = uiState.sortMode == FilePickerSortMode.NAME,
                            onClick = {
                                showSortMenu = false
                                onUiEvent(FilePickerUiEvent.SortSelected(FilePickerSortMode.NAME))
                            }
                        )
                        SortItem(
                            title = stringResource(Res.string.feature_media_file_picker_sort_size),
                            selected = uiState.sortMode == FilePickerSortMode.SIZE,
                            onClick = {
                                showSortMenu = false
                                onUiEvent(FilePickerUiEvent.SortSelected(FilePickerSortMode.SIZE))
                            }
                        )
                        SortItem(
                            title = stringResource(Res.string.feature_media_file_picker_sort_type),
                            selected = uiState.sortMode == FilePickerSortMode.TYPE,
                            onClick = {
                                showSortMenu = false
                                onUiEvent(FilePickerUiEvent.SortSelected(FilePickerSortMode.TYPE))
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SortItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.spacing.base))
                }
                Text(title)
            }
        },
        onClick = onClick
    )
}

@Composable
private fun FilePickerBottomBar(
    containerColor: Color,
    uiState: FilePickerUiState,
    onUiEvent: (FilePickerUiEvent) -> Unit
) {
    if (uiState.requiresFileAccess || uiState.selectionCapacity <= 0) return
    Surface(color = containerColor) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.base
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text =
                    stringResource(
                        Res.string.feature_media_file_picker_selected,
                        uiState.selectedCount,
                        uiState.selectionCapacity
                    ),
                style = MaterialTheme.typography.bodyMedium
            )
            SparrowApprovalButton(
                onClick = { onUiEvent(FilePickerUiEvent.ConfirmClicked) },
                enabled = uiState.canConfirm,
                fillMaxWidth = false,
                content = {
                    if (uiState.isConfirming) {
                        CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.spacing.medium))
                    } else {
                        Text(stringResource(Res.string.feature_media_file_picker_add_files))
                    }
                }
            )
        }
    }
}

private fun FileBrowserEntryKind.icon(): ImageVector =
    when (this) {
        FileBrowserEntryKind.DIRECTORY -> Icons.Default.Folder
        FileBrowserEntryKind.IMAGE -> Icons.Default.Image
        FileBrowserEntryKind.VIDEO -> Icons.Default.Movie
        FileBrowserEntryKind.AUDIO -> Icons.Default.AudioFile
        FileBrowserEntryKind.PDF -> Icons.Default.PictureAsPdf
        FileBrowserEntryKind.TEXT -> Icons.Default.Description
        FileBrowserEntryKind.ARCHIVE -> Icons.Default.Archive
        FileBrowserEntryKind.OTHER -> Icons.Default.InsertDriveFile
    }
