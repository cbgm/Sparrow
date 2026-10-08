package com.cbgm.sparrow.feature.polls.presentation.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.polls.presentation.component.pollDurationLabel
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_EXPIRY_MINUTES
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_allow_multiple
import com.cbgm.sparrow.resources.feature_polls_allow_vote_change
import com.cbgm.sparrow.resources.feature_polls_anonymous
import com.cbgm.sparrow.resources.feature_polls_anonymous_hint
import com.cbgm.sparrow.resources.feature_polls_duration_conversion
import com.cbgm.sparrow.resources.feature_polls_expiry_hint
import com.cbgm.sparrow.resources.feature_polls_expiry_invalid
import com.cbgm.sparrow.resources.feature_polls_expiry_minutes
import com.cbgm.sparrow.resources.feature_polls_expiry_minutes_placeholder
import com.cbgm.sparrow.resources.feature_polls_poll_ends
import org.jetbrains.compose.resources.stringResource

@Composable
fun PollSettingsSection(
    expiryEnabled: Boolean,
    expiryMinutes: String,
    expiryInvalid: Boolean,
    allowMultipleSelection: Boolean,
    allowVoteChange: Boolean,
    isAnonymous: Boolean,
    onExpiryEnabledChanged: (Boolean) -> Unit,
    onExpiryMinutesChanged: (String) -> Unit,
    onMultipleSelectionChanged: (Boolean) -> Unit,
    onVoteChangeChanged: (Boolean) -> Unit,
    onAnonymousChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                ) {
                    SparrowInputField(
                        value = expiryMinutes,
                        onValueChange = onExpiryMinutesChanged,
                        label = stringResource(Res.string.feature_polls_expiry_minutes),
                        placeholderText = stringResource(Res.string.feature_polls_expiry_minutes_placeholder),
                        modifier = Modifier.weight(1f),
                        isSingleLine = true,
                        isError = expiryInvalid,
                        errorText = stringResource(Res.string.feature_polls_expiry_invalid),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    expiryMinutes.toLongOrNull()?.takeIf { it in 1L..MAX_EXPIRY_MINUTES }
                        ?.let { minutes ->
                            Text(
                                text = stringResource(
                                    Res.string.feature_polls_duration_conversion,
                                    pollDurationLabel(minutes)
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
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
            PollSettingSwitchRow(
                title = stringResource(Res.string.feature_polls_anonymous),
                description = stringResource(Res.string.feature_polls_anonymous_hint),
                checked = isAnonymous,
                onCheckedChange = onAnonymousChanged
            )
        }
    }
}

@Composable
private fun PollSettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                expiryMinutes = "3600",
                expiryInvalid = false,
                allowMultipleSelection = true,
                allowVoteChange = true,
                isAnonymous = false,
                onExpiryEnabledChanged = {},
                onExpiryMinutesChanged = {},
                onMultipleSelectionChanged = {},
                onVoteChangeChanged = {},
                onAnonymousChanged = {}
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
                title = "Anonymous voting",
                description = "Voter identities are hidden from participants.",
                checked = true,
                onCheckedChange = {}
            )
        }
    }
}
