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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.PatternBackground
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiState
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
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
import com.cbgm.sparrow.resources.feature_contactimport_scan_qr_code
import com.cbgm.sparrow.resources.feature_contactimport_shared_identity
import org.jetbrains.compose.resources.stringResource

private val Field = Color(0xFF102A46)

@Composable
fun ImportIdentityScreen(
    uiState: ImportIdentityUiState,
    onUiEvent: (ImportIdentityUiEvent) -> Unit,
    importContactId: String?,
    modifier: Modifier = Modifier
) {
    SparrowScrollScaffold(
        modifier = modifier,
        background = {
            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = MaterialTheme.colorScheme.background,
                alpha = 0.04f
            )
        },
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Text(
                text = stringResource(Res.string.feature_contactimport_in_person_qr_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(Res.string.feature_contactimport_in_person_qr_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            OutlinedButton(
                onClick = { onUiEvent(ImportIdentityUiEvent.ScanQrCodeClicked) },
                enabled = !uiState.isImporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.ImportIdentityScreen.headerIconSize)
                )

                Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

                Text(text = stringResource(Res.string.feature_contactimport_scan_qr_code))
            }

            ManualInputDivider()

            Text(
                text = stringResource(Res.string.feature_contactimport_paste_identity_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(Res.string.feature_contactimport_paste_shared_identity_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            OutlinedTextField(
                value = uiState.encodedIdentity,
                onValueChange = { value ->
                    onUiEvent(ImportIdentityUiEvent.EncodedIdentityChanged(value))
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(Res.string.feature_contactimport_shared_identity))
                },
                minLines = 4,
                enabled = !uiState.isImporting,
                textStyle =
                    MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedContainerColor = Field,
                        unfocusedContainerColor = Field,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        cursorColor = MaterialTheme.colorScheme.secondary
                    )
            )

            ImportButton(
                isImporting = uiState.isImporting,
                enabled = uiState.encodedIdentity.isNotBlank(),
                onClick = {
                    onUiEvent(
                        ImportIdentityUiEvent.ImportClicked(
                            contactId = importContactId,
                            identityImportTrust = IdentityImportTrust.UNVERIFIED
                        )
                    )
                }
            )

            uiState.importedContactName?.let { name ->
                val statusText =
                    when (uiState.importedIdentityTrust) {
                        IdentityImportTrust.VERIFIED_IN_PERSON -> {
                            stringResource(Res.string.feature_contactimport_imported_verified_name, name)
                        }

                        IdentityImportTrust.UNVERIFIED -> {
                            stringResource(Res.string.feature_contactimport_imported_unverified_name, name)
                        }

                        null -> {
                            stringResource(Res.string.feature_contactimport_imported_name, name)
                        }
                    }

                StatusBanner(
                    icon = Icons.Default.CheckCircle,
                    text = statusText,
                    color =
                        if (uiState.importedIdentityTrust == IdentityImportTrust.VERIFIED_IN_PERSON) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.tertiary
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
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_contactimport_import_identity),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun ManualInputDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
        )

        Text(
            text = stringResource(Res.string.feature_contactimport_or_paste_manually),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.base
                )
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun ImportButton(
    isImporting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isImporting && enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.background,
                disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                disabledContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
            )
    ) {
        if (isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.ImportIdentityScreen.progressSize),
                strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                color = MaterialTheme.colorScheme.background
            )
        } else {
            Text(
                text = stringResource(Res.string.feature_contactimport_import_unverified_identity),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusBanner(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Dimens.ImportIdentityScreen.resultIconSize)
            )

            Spacer(modifier = Modifier.size(10.dp))

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
            onUiEvent = {},
            importContactId = null
        )
    }
}
