package com.cbgm.sparrow.feature.identity.presentation.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiEvent
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiState
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_identity_backup_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityBackupScreen(
    uiState: IdentityBackupUiState,
    modifier: Modifier = Modifier,
    onUiEvent: (IdentityBackupUiEvent) -> Unit,
    onExportIdentity: () -> Unit = {}
) {
    SparrowScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = stringResource(Res.string.feature_identity_backup_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(IdentityBackupUiEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.base_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding, scrollState ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.screenPadding,
                    bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.screenPadding,
                    start = MaterialTheme.spacing.screenPadding,
                    end = MaterialTheme.spacing.screenPadding
                )
        ) {
            SparrowCardNoAnimation {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                    IdentityBackupSection(
                        state = IdentityBackupUiState(
                            status = when (uiState.status) {
                                IdentityBackupUiStatus.NOT_BACKED_UP -> IdentityBackupUiStatus.NOT_BACKED_UP
                                IdentityBackupUiStatus.EXPORTED -> IdentityBackupUiStatus.EXPORTED
                                IdentityBackupUiStatus.IMPORTED -> IdentityBackupUiStatus.IMPORTED
                            },
                            busy = uiState.busy,
                            message = uiState.message,
                            error = uiState.error
                        ),
                        onExport = onExportIdentity
                    )
                }
            }
        }
    }
}
