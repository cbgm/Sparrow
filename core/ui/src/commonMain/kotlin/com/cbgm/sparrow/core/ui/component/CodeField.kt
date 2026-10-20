package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

/** A one-time code shown as one box per digit, as in SMS verification screens. */
@Composable
fun SparrowCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isEnabled: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isEnabled) {
        if (isEnabled) focusRequester.requestFocus()
    }
    BasicTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(length)) },
        modifier = modifier.fillMaxWidth().focusRequester(focusRequester),
        enabled = isEnabled,
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        decorationBox = {
            // Equal boxes across the available width, so six digits fit any dialog.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                repeat(length) { index ->
                    val digit = value.getOrNull(index)
                    val isNext = index == value.length && isEnabled
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(BOX_ASPECT_RATIO)
                                .border(
                                    width = if (isNext) 2.dp else 1.dp,
                                    color =
                                        if (isNext || digit != null) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                    shape = MaterialTheme.shapes.small
                                ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit?.toString().orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

private const val BOX_ASPECT_RATIO = 0.8f

@Preview
@Composable
private fun SparrowCodeFieldPreview() {
    SparrowTheme {
        SparrowCodeField(value = "1234", onValueChange = {})
    }
}
