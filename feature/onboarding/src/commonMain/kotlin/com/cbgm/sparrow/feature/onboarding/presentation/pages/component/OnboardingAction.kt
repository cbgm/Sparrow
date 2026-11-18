package com.cbgm.sparrow.feature.onboarding.presentation.pages.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.bleedHorizontally
import com.cbgm.sparrow.core.ui.component.sparrowRipple
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

private const val ACTION_FADE_MILLISECONDS = 800L

@Composable
fun OnboardingAction(
    text: String,
    clock: State<Long>,
    startMillis: Long,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OnboardingButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.graphicsLayer { alpha = clock.progress(startMillis, ACTION_FADE_MILLISECONDS) }
    )
}

@Composable
private fun OnboardingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lineWidth by animateDpAsState(
        if (pressed) Dimens.Button.actionLinePressedWidth else Dimens.Button.actionLineWidth,
        label = "actionLine"
    )
    Row(
        modifier =
            modifier
                .bleedHorizontally(Dimens.Button.actionPadding)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(
                    interactionSource = interaction,
                    indication = sparrowRipple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                )
                .heightIn(min = Dimens.Button.minTouchHeight)
                .padding(horizontal = Dimens.Button.actionPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) textColor else MaterialTheme.colorScheme.outline
        )
        Box(
            Modifier
                .size(lineWidth, Dimens.Button.actionLineThickness)
                .background(if (enabled) lineColor else MaterialTheme.colorScheme.outline)
        )
    }
}

@Preview
@Composable
private fun OnboardingButtonPreview() {
    SparrowTheme {
        Column {
            OnboardingButton(text = "Allow", onClick = {})
            OnboardingButton(text = "Create", onClick = {}, enabled = false)
        }
    }
}
