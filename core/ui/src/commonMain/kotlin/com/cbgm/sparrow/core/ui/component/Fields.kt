package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

/*
 * Every input in the app looks the same: no box, the text on a hairline that turns cyan while typing
 * (red on an error), thin icons and a cyan cursor. A label, when there is one, sits small above.
 */

@Composable
fun SparrowSearchField(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester ?: FocusRequester.Cancel),
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            FieldBox(
                focused = focused,
                leading = {
                    Icon(
                        imageVector = SparrowIcons.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(Dimens.SearchField.searchIconSize)
                    )
                },
                trailing = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(Dimens.SearchField.searchClearButtonSize)
                        ) {
                            Icon(
                                imageVector = SparrowIcons.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(Dimens.SearchField.searchIconSize)
                            )
                        }
                    }
                }
            ) {
                FieldText(
                    isEmpty = searchQuery.isEmpty(),
                    placeholder = placeholder,
                    innerTextField = innerTextField
                )
            }
        }
    )
}

@Composable
fun SparrowInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholderText: String = "",
    errorText: String = "",
    isSingleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (isSingleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    isEnabled: Boolean = true,
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRequester: FocusRequester? = null
) {
    val defaultKeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done
    )
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.micro),
                style = MaterialTheme.typography.labelMedium,
                color =
                    when {
                        isError -> MaterialTheme.colorScheme.error
                        focused -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester ?: FocusRequester.Cancel),
            enabled = isEnabled,
            singleLine = isSingleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color =
                    if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
            ),
            keyboardOptions = keyboardOptions ?: defaultKeyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                FieldBox(focused = focused, isError = isError) {
                    FieldText(
                        isEmpty = value.isEmpty(),
                        placeholder = placeholderText,
                        innerTextField = innerTextField
                    )
                }
            }
        )
        if (isError && errorText.isNotBlank()) {
            Text(
                text = errorText,
                modifier = Modifier.padding(top = MaterialTheme.spacing.micro),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun FieldBox(
    focused: Boolean,
    isError: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val line by animateColorAsState(
        when {
            isError -> MaterialTheme.colorScheme.error
            focused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "fieldLine"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.SearchField.searchHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            leading?.invoke()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = MaterialTheme.spacing.base)
            ) {
                content()
            }
            trailing?.invoke()
        }
        HorizontalDivider(thickness = Dimens.Base.borderStrokeWidth, color = line)
    }
}

@Composable
private fun FieldText(
    isEmpty: Boolean,
    placeholder: String,
    innerTextField: @Composable () -> Unit
) {
    if (isEmpty && placeholder.isNotBlank()) {
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
    innerTextField()
}

@Preview
@Composable
fun SearchFieldPreview() {
    SparrowTheme {
        Column(
            Modifier.fillMaxWidth()
        ) {
            SparrowSearchField(
                searchQuery = "test",
                onSearchQueryChanged = {},
                placeholder = "Test",
                onClear = {}
            )
            SparrowInputField(
                value = "Test",
                onValueChange = {},
                isEnabled = true,
                label = "Test"
            )
        }
    }
}
