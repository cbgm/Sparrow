package com.cbgm.sparrow.feature.onboarding.presentation.pages.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.component.bleedHorizontally
import com.cbgm.sparrow.core.ui.component.sparrowRipple
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
internal fun OnboardingChoiceRow(
    title: String,
    consequence: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val titleColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline, label = "choiceTitle")
    val markColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "choiceMark")
    // The ripple spans the screen from edge to edge; the text stays in line with the page.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .bleedHorizontally(Dimens.OnboardingScreen.horizontalPadding)
                .selectable(selected = selected, interactionSource = null, indication = sparrowRipple(), role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = Dimens.OnboardingScreen.horizontalPadding)
    ) {
        Row(
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(top = MaterialTheme.spacing.base)
                        .size(Dimens.OnboardingScreen.choiceMarkSize)
                        .background(markColor, CircleShape)
            )
            Column {
                Text(text = title, style = onboardingBodyStyle().copy(fontWeight = FontWeight.Normal), color = titleColor)
                consequence?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
