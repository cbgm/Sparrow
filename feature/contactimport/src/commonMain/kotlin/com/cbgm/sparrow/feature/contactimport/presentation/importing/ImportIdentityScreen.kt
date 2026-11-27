package com.cbgm.sparrow.feature.contactimport.presentation.importing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.IdentityImportTrustUi
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_import_identity
import com.cbgm.sparrow.resources.feature_contactimport_import_unverified_identity
import com.cbgm.sparrow.resources.feature_contactimport_imported_name
import com.cbgm.sparrow.resources.feature_contactimport_imported_unverified_name
import com.cbgm.sparrow.resources.feature_contactimport_imported_verified_name
import com.cbgm.sparrow.resources.feature_contactimport_in_person_qr_description
import com.cbgm.sparrow.resources.feature_contactimport_in_person_qr_title
import com.cbgm.sparrow.resources.feature_contactimport_normal_invitation_description
import com.cbgm.sparrow.resources.feature_contactimport_or_paste_manually
import com.cbgm.sparrow.resources.feature_contactimport_paste_identity_title
import com.cbgm.sparrow.resources.feature_contactimport_paste_shared_identity_description
import com.cbgm.sparrow.resources.feature_contactimport_shared_identity
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImportIdentityScreen(
    uiState: ImportIdentityUiState,
    onUiEvent: (ImportIdentityUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    scanner: @Composable (enabled: Boolean) -> Unit = {}
) {
    SparrowScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            ImportIdentityTopBar(
                containerColor = containerColor,
                onBack = { onUiEvent(ImportIdentityUiEvent.BackClicked) }
            )
        }
    ) { innerPadding, scrollState ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                        start = MaterialTheme.spacing.screenPadding,
                        end = MaterialTheme.spacing.screenPadding
                    ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_contactimport_normal_invitation_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
            )

            Column(
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(Res.string.feature_contactimport_in_person_qr_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = stringResource(Res.string.feature_contactimport_in_person_qr_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
                )

                scanner(!uiState.isImporting)
            }

            Text(
                text = stringResource(Res.string.feature_contactimport_or_paste_manually),
                modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(Res.string.feature_contactimport_paste_identity_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = stringResource(Res.string.feature_contactimport_paste_shared_identity_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
                )

                SparrowInputField(
                    value = uiState.encodedIdentity,
                    onValueChange = { value ->
                        onUiEvent(ImportIdentityUiEvent.EncodedIdentityChanged(value))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(Res.string.feature_contactimport_shared_identity),
                    minLines = 1,
                    isEnabled = !uiState.isImporting
                )

                ImportButton(
                    isImporting = uiState.isImporting,
                    enabled = uiState.encodedIdentity.isNotBlank(),
                    onClick = {
                        onUiEvent(ImportIdentityUiEvent.ImportClicked)
                    }
                )
            }

            uiState.importedContactName?.let { name ->
                val statusText =
                    when (uiState.importedIdentityTrust) {
                        IdentityImportTrustUi.VERIFIED_IN_PERSON -> {
                            stringResource(
                                Res.string.feature_contactimport_imported_verified_name,
                                name
                            )
                        }

                        IdentityImportTrustUi.UNVERIFIED -> {
                            stringResource(
                                Res.string.feature_contactimport_imported_unverified_name,
                                name
                            )
                        }

                        null -> {
                            stringResource(Res.string.feature_contactimport_imported_name, name)
                        }
                    }

                StatusBanner(
                    icon = Icons.Default.CheckCircle,
                    text = statusText,
                    color =
                        if (uiState.importedIdentityTrust == IdentityImportTrustUi.VERIFIED_IN_PERSON) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                )
            }

            uiState.errorMessage?.let { message ->
                StatusBanner(
                    icon = Icons.Default.ErrorOutline,
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ImportIdentityTopBar(
    containerColor: Color,
    onBack: () -> Unit
) {
    SparrowTopBar(
        containerColor = containerColor,
        title = {
            Text(
                text = stringResource(Res.string.feature_contactimport_import_identity)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = SparrowIcons.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun ImportButton(
    isImporting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    SparrowApprovalButton(
        onClick = onClick,
        enabled = !isImporting && enabled,
        text =
            if (isImporting) {
                ""
            } else {
                stringResource(Res.string.feature_contactimport_import_unverified_identity)
            },
        content = {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.ImportIdentityScreen.progressSize),
                    strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                    color = LocalContentColor.current
                )
            }
        }
    )
}

@Composable
private fun StatusBanner(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = Alpha.ImportIdentityScreen.iconBackground)
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.importIdentityScreen.resultVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Dimens.ImportIdentityScreen.resultIconSize)
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.importIdentityScreen.resultIconGap))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview
@Composable
fun ImportIdentityScreenPreview() {
    SparrowTheme {
        ImportIdentityScreen(
            uiState = ImportIdentityUiState(),
            onUiEvent = {}
        )
    }
}
