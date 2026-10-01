package com.cbgm.sparrow.feature.polls.presentation.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_allow_multiple
import com.cbgm.sparrow.resources.feature_polls_allow_vote_change
import com.cbgm.sparrow.resources.feature_polls_expiry_date
import com.cbgm.sparrow.resources.feature_polls_expiry_date_placeholder
import com.cbgm.sparrow.resources.feature_polls_expiry_hint
import com.cbgm.sparrow.resources.feature_polls_expiry_invalid
import com.cbgm.sparrow.resources.feature_polls_expiry_time
import com.cbgm.sparrow.resources.feature_polls_expiry_time_placeholder
import com.cbgm.sparrow.resources.feature_polls_poll_ends
import com.cbgm.sparrow.resources.feature_polls_settings
import org.jetbrains.compose.resources.stringResource

@Composable
fun PollSettingsSection(
    expiryEnabled: Boolean,
    expiryDate: String,
    expiryTime: String,
    expiryInvalid: Boolean,
    allowMultipleSelection: Boolean,
    allowVoteChange: Boolean,
    onExpiryEnabledChanged: (Boolean) -> Unit,
    onExpiryDateChanged: (String) -> Unit,
    onExpiryTimeChanged: (String) -> Unit,
    onMultipleSelectionChanged: (Boolean) -> Unit,
    onVoteChangeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Text(
            text = stringResource(Res.string.feature_polls_settings),
            style = MaterialTheme.typography.titleSmall
        )
        SparrowCardNoAnimation {
            Column(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                PollSettingSwitchRow(
                    title = stringResource(Res.string.feature_polls_poll_ends),
                    checked = expiryEnabled,
                    onCheckedChange = onExpiryEnabledChanged
                )
                if (expiryEnabled) {
                    Text(
                        text = stringResource(Res.string.feature_polls_expiry_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                    ) {
                        SparrowInputField(
                            value = expiryDate,
                            onValueChange = onExpiryDateChanged,
                            label = stringResource(Res.string.feature_polls_expiry_date),
                            placeholderText = stringResource(Res.string.feature_polls_expiry_date_placeholder),
                            modifier = Modifier.weight(1f),
                            isSingleLine = true,
                            isError = expiryInvalid,
                            errorText = stringResource(Res.string.feature_polls_expiry_invalid)
                        )
                        SparrowInputField(
                            value = expiryTime,
                            onValueChange = onExpiryTimeChanged,
                            label = stringResource(Res.string.feature_polls_expiry_time),
                            placeholderText = stringResource(Res.string.feature_polls_expiry_time_placeholder),
                            modifier = Modifier.weight(1f),
                            isSingleLine = true,
                            isError = expiryInvalid,
                            errorText = stringResource(Res.string.feature_polls_expiry_invalid)
                        )
                    }
                }
                PollSettingSwitchRow(
                    title = stringResource(Res.string.feature_polls_allow_multiple),
                    checked = allowMultipleSelection,
                    onCheckedChange = onMultipleSelectionChanged
                )
                PollSettingSwitchRow(
                    title = stringResource(Res.string.feature_polls_allow_vote_change),
                    checked = allowVoteChange,
                    onCheckedChange = onVoteChangeChanged
                )
            }
        }
    }
}

@Composable
private fun PollSettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview
@Composable
private fun PollSettingsSectionPreview() {
    SparrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PollSettingsSection(
                expiryEnabled = true,
                expiryDate = "2026-10-01",
                expiryTime = "18:00",
                expiryInvalid = false,
                allowMultipleSelection = true,
                allowVoteChange = true,
                onExpiryEnabledChanged = {},
                onExpiryDateChanged = {},
                onExpiryTimeChanged = {},
                onMultipleSelectionChanged = {},
                onVoteChangeChanged = {}
            )
        }
    }
}

@Preview
@Composable
private fun PollSettingSwitchRowPreview() {
    SparrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PollSettingSwitchRow(
                title = "Allow multiple answers",
                checked = true,
                onCheckedChange = {}
            )
        }
    }
}
