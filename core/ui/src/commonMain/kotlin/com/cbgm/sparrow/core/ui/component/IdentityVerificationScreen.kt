package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_verify_contact
import com.cbgm.sparrow.resources.feature_chats_compare_safety_number_contact
import com.cbgm.sparrow.resources.feature_chats_confirm_matching_numbers_only
import com.cbgm.sparrow.resources.feature_chats_numbers_match
import com.cbgm.sparrow.resources.feature_chats_or_compare_safety_number
import com.cbgm.sparrow.resources.feature_chats_safety_number_unavailable
import com.cbgm.sparrow.resources.feature_chats_scan_identity_qr
import com.cbgm.sparrow.resources.feature_chats_scan_identity_qr_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityVerificationScreen(
    contactName: String,
    safetyNumber: String,
    isLoadingSafetyNumber: Boolean,
    isVerifying: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onScanQrCode: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BlockScreenshotEffect(enabled = true)

    SparrowScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            VerificationTopBar(
                title = stringResource(Res.string.base_verify_contact, contactName),
                onBack = onBack,
                containerColor = containerColor
            )
        }
    ) { innerPadding, scrollState ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.screenPadding,
                        bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.screenPadding,
                        start = MaterialTheme.spacing.screenPadding,
                        end = MaterialTheme.spacing.screenPadding
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_scan_identity_qr_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            SparrowSecondaryButton(
                onClick = onScanQrCode,
                enabled = !isVerifying,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IdentityVerificationScreen.iconSize)
                    )

                    Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

                    Text(text = stringResource(Res.string.feature_chats_scan_identity_qr))
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(Res.string.feature_chats_or_compare_safety_number),
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Text(
                text =
                    stringResource(
                        Res.string.feature_chats_compare_safety_number_contact,
                        contactName
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            SafetyNumberContent(
                safetyNumber = safetyNumber,
                isLoadingSafetyNumber = isLoadingSafetyNumber
            )

            Text(
                text = stringResource(Res.string.feature_chats_confirm_matching_numbers_only),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            SparrowApprovalButton(
                onClick = onConfirm,
                enabled = !isVerifying && !isLoadingSafetyNumber && safetyNumber.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                content = {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.IdentityVerificationScreen.iconSize),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.feature_chats_numbers_match),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SafetyNumberContent(
    safetyNumber: String,
    isLoadingSafetyNumber: Boolean
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingSafetyNumber -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

            safetyNumber.isBlank() -> {
                Text(
                    text = stringResource(Res.string.feature_chats_safety_number_unavailable),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            else -> {
                Text(
                    text = safetyNumber,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(vertical = MaterialTheme.spacing.small),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VerificationTopBar(
    title: String,
    containerColor: Color,
    onBack: () -> Unit
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
        }
    )
}

@Preview
@Composable
private fun IdentityVerificationScreenPreview() {
    SparrowTheme {
        IdentityVerificationScreen(
            contactName = "dsffsdf",
            safetyNumber = "654654654",
            isLoadingSafetyNumber = false,
            isVerifying = false,
            errorMessage = null,
            onConfirm = {},
            onScanQrCode = {},
            onBack = {}
        )
    }
}
