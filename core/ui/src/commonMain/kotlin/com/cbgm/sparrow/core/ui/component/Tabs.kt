package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun SparrowTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = Dimens.Tabs.height,
    isFullUnderLine: Boolean = false
) {
    Row(modifier = modifier.fillMaxWidth().height(height)) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(
                        selected = isSelected,
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = Dimens.Tabs.letterSpacing,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                )
                Box(
                    modifier = Modifier
                        .padding(top = MaterialTheme.spacing.micro)
                        .then(
                            if (!isFullUnderLine) {
                                Modifier.width(Dimens.Tabs.lineWidth)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        )
                        .height(Dimens.Tabs.lineHeight)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SparrowTabsPreview() {
    SparrowTheme {
        SparrowTabs(labels = listOf("Chats", "Me", "Settings"), selectedIndex = 0, onSelect = {})
    }
}
