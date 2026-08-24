package com.cbgm.sparrow.feature.safety.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningLevel
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningReason
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUiModel
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_safety_high_risk_message
import com.cbgm.sparrow.resources.feature_safety_suspicious_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageSafetyWarning(
    warning: MessageSafetyWarningUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor =
        when (warning.level) {
            MessageSafetyWarningLevel.SUSPICIOUS ->
                MaterialTheme.colorScheme.error.copy(alpha = Alpha.Watermark)

            MessageSafetyWarningLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
        }
    val contentColor =
        when (warning.level) {
            MessageSafetyWarningLevel.SUSPICIOUS -> MaterialTheme.colorScheme.error
            MessageSafetyWarningLevel.HIGH -> MaterialTheme.colorScheme.onErrorContainer
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.micro
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(Dimens.MessageSafetyWarning.iconSize)
            )
            Text(
                text = warningTitle(warning.level),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun warningTitle(level: MessageSafetyWarningLevel): String =
    when (level) {
        MessageSafetyWarningLevel.SUSPICIOUS ->
            stringResource(Res.string.feature_safety_suspicious_message)

        MessageSafetyWarningLevel.HIGH ->
            stringResource(Res.string.feature_safety_high_risk_message)
    }

@Preview
@Composable
private fun MessageSafetyWarningPreview() {
    SparrowTheme {
        MessageSafetyWarning(
            warning =
                MessageSafetyWarningUiModel(
                    level = MessageSafetyWarningLevel.SUSPICIOUS,
                    reasons = listOf(MessageSafetyWarningReason.SUSPICIOUS_LINK)
                ),
            onClick = {}
        )
    }
}
