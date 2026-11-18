package com.cbgm.sparrow.feature.polls.presentation.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.polls.presentation.create.model.PollOptionEditorUi
import com.cbgm.sparrow.feature.polls.util.PollConstants
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_add_option
import com.cbgm.sparrow.resources.feature_polls_option
import com.cbgm.sparrow.resources.feature_polls_remove_option
import org.jetbrains.compose.resources.stringResource

@Composable
fun PollOptionsEditor(
    options: List<PollOptionEditorUi>,
    onOptionChanged: (String, String) -> Unit,
    onRemoveOption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        options.forEachIndexed { index, option ->
            val removable = options.size > PollConstants.MIN_OPTIONS &&
                !(index == options.lastIndex && option.text.isBlank())
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
            ) {
                SparrowInputField(
                    value = option.text,
                    onValueChange = { value -> onOptionChanged(option.id, value) },
                    label = stringResource(Res.string.feature_polls_option, index + 1),
                    modifier = Modifier.weight(1f),
                    placeholderText = stringResource(Res.string.feature_polls_add_option),
                    isSingleLine = true
                )
                IconButton(
                    onClick = { onRemoveOption(option.id) },
                    enabled = removable,
                    modifier = Modifier
                        .size(Dimens.Button.iconButtonSize)
                        .alpha(if (removable) 1f else 0f)
                ) {
                    Icon(
                        imageVector = SparrowIcons.Close,
                        contentDescription = stringResource(Res.string.feature_polls_remove_option),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PollOptionsEditorPreview() {
    SparrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PollOptionsEditor(
                options =
                    listOf(
                        PollOptionEditorUi("1", "Go hiking"),
                        PollOptionEditorUi("2", "Visit a city"),
                        PollOptionEditorUi("3", "Stay at home")
                    ),
                onOptionChanged = { _, _ -> },
                onRemoveOption = {}
            )
        }
    }
}
