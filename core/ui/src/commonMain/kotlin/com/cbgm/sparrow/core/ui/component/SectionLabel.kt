package com.cbgm.sparrow.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme

/** The section heading of the app: small cyan capitals with a little air between the letters. */
@Composable
fun SparrowSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = Dimens.SectionLabel.letterSpacing,
        color = color ?: MaterialTheme.colorScheme.primary
    )
}

@Preview
@Composable
private fun SparrowSectionLabelPreview() {
    SparrowTheme {
        SparrowSectionLabel(text = "Members")
    }
}
