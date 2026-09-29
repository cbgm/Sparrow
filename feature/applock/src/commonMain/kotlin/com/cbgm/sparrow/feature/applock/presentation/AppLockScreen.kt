package com.cbgm.sparrow.feature.applock.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.BlockScreenshotEffect
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_applock_unlock_sparrow
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppLockScreen(
    isAuthenticating: Boolean,
    onUnlockRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    BlockScreenshotEffect(enabled = true)

    val pulse = rememberAppLockPulse()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    enabled = !isAuthenticating,
                    onClick = onUnlockRequested
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = stringResource(Res.string.feature_applock_unlock_sparrow),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(Dimens.AppLockScreen.iconSize)
                .graphicsLayer {
                    scaleX = pulse.scale
                    scaleY = pulse.scale
                }
                .alpha(pulse.alpha)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_applock_unlock_sparrow),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun rememberAppLockPulse(): AppLockPulse {
    val transition = rememberInfiniteTransition(label = "appLockPulse")

    val scale by
        transition.animateFloat(
            initialValue = LOCK_PULSE_MIN_SCALE,
            targetValue = LOCK_PULSE_MAX_SCALE,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = LOCK_PULSE_DURATION_MILLIS),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "appLockScale"
        )

    val alpha by
        transition.animateFloat(
            initialValue = LOCK_PULSE_MIN_ALPHA,
            targetValue = LOCK_PULSE_MAX_ALPHA,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = LOCK_PULSE_DURATION_MILLIS),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "appLockAlpha"
        )

    return AppLockPulse(
        scale = scale,
        alpha = alpha
    )
}

private data class AppLockPulse(
    val scale: Float,
    val alpha: Float
)

private const val LOCK_PULSE_DURATION_MILLIS = 1_000
private const val LOCK_PULSE_MIN_SCALE = 0.92f
private const val LOCK_PULSE_MAX_SCALE = 1.08f
private const val LOCK_PULSE_MIN_ALPHA = 0.55f
private const val LOCK_PULSE_MAX_ALPHA = 1f

@Preview
@Composable
private fun AppLockScreenPreview() {
    SparrowTheme {
        AppLockScreen(
            isAuthenticating = false,
            onUnlockRequested = {}
        )
    }
}
