package com.cbgm.sparrow.feature.identity.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.BlockScreenshotEffect
import com.cbgm.sparrow.core.ui.component.SparrowBanner
import com.cbgm.sparrow.core.ui.component.SparrowHoldAction
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_verify_contact
import com.cbgm.sparrow.resources.feature_chats_confirm_matching_numbers_only
import com.cbgm.sparrow.resources.feature_chats_numbers_match
import com.cbgm.sparrow.resources.feature_chats_or_compare_safety_number
import com.cbgm.sparrow.resources.feature_chats_safety_number_unavailable
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityVerificationScreen(
    contactName: String,
    safetyNumber: String,
    isLoadingSafetyNumber: Boolean,
    isVerifying: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    scanner: @Composable (enabled: Boolean) -> Unit,
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
                        top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.base,
                        bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.screenPadding,
                        start = MaterialTheme.spacing.screenPadding,
                        end = MaterialTheme.spacing.screenPadding
                    ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            scanner(!isVerifying)
            Text(
                text = stringResource(Res.string.feature_chats_or_compare_safety_number),
                modifier = Modifier.padding(top = MaterialTheme.spacing.medium, bottom = MaterialTheme.spacing.micro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SafetyNumberContent(
                safetyNumber = safetyNumber,
                isLoadingSafetyNumber = isLoadingSafetyNumber
            )
            Text(
                text = stringResource(Res.string.feature_chats_confirm_matching_numbers_only),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )

            if (errorMessage != null) {
                SparrowBanner(
                    icon = Icons.Default.ErrorOutline,
                    accent = MaterialTheme.colorScheme.error,
                    title = errorMessage
                )
            }

            SparrowHoldAction(
                label = stringResource(Res.string.feature_chats_numbers_match),
                onConfirm = onConfirm,
                enabled = !isLoadingSafetyNumber && safetyNumber.isNotBlank(),
                isBusy = isVerifying,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = MaterialTheme.spacing.large)
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
                SparrowCodeGrid(
                    groups = safetyNumber.split(" "),
                    columns = SafetyNumberColumns,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

private const val SafetyNumberColumns = 4

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VerificationTopBar(
    title: String,
    containerColor: Color,
    onBack: () -> Unit
) {
    SparrowTopBar(
        containerColor = containerColor,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = SparrowIcons.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.TopBar.iconSize)
                )
            }
        },
        title = {
            Text(
                text = title,
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
            safetyNumber = "02369 58689 47536 43636 49498 57165 50772 05009 65297 59824 27402 50558 10875 40465 23483 08260",
            isLoadingSafetyNumber = false,
            isVerifying = false,
            errorMessage = null,
            onConfirm = {},
            scanner = {},
            onBack = {}
        )
    }
}
