package com.cbgm.sparrow.feature.polls.presentation.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.polls.presentation.create.model.PollOptionEditorUi
import com.cbgm.sparrow.feature.polls.util.PollConstants
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_add_option
import com.cbgm.sparrow.resources.feature_polls_option
import com.cbgm.sparrow.resources.feature_polls_options
import com.cbgm.sparrow.resources.feature_polls_remove_option
import org.jetbrains.compose.resources.stringResource

@Composable
fun PollOptionsEditor(
    options: List<PollOptionEditorUi>,
    onOptionChanged: (String, String) -> Unit,
    onRemoveOption: (String) -> Unit,
    onAddOption: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Text(
            text = stringResource(Res.string.feature_polls_options),
            style = MaterialTheme.typography.titleSmall
        )

        options.forEachIndexed { index, option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
            ) {
                SparrowInputField(
                    value = option.text,
                    onValueChange = { value -> onOptionChanged(option.id, value) },
                    label = stringResource(Res.string.feature_polls_option, index + 1),
                    modifier = Modifier.weight(1f),
                    isSingleLine = true
                )
                IconButton(
                    onClick = { onRemoveOption(option.id) },
                    enabled = options.size > PollConstants.MIN_OPTIONS,
                    modifier = Modifier.size(Dimens.Button.iconButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.feature_polls_remove_option)
                    )
                }
            }
        }

        TextButton(onClick = onAddOption) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Text(text = stringResource(Res.string.feature_polls_add_option))
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
                onRemoveOption = {},
                onAddOption = {}
            )
        }
    }
}
