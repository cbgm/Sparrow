package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

@Composable
fun SparrowInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholderText: String = "",
    errorText: String = "",
    supportingText: String = "",
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
        val showError = isError && errorText.isNotBlank()
        if (showError || supportingText.isNotBlank()) {
            Text(
                text = if (showError) errorText else supportingText,
                modifier = Modifier.padding(top = MaterialTheme.spacing.micro),
                style = MaterialTheme.typography.labelMedium,
                color = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SparrowSelectField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.micro),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        FieldBox(
            focused = false,
            trailing = {
                Icon(
                    imageVector = SparrowIcons.Chevron,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Dimens.SettingsScreen.secondaryIconSize)
                        .rotate(QuarterTurn),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val QuarterTurn = 90f

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

private const val BOX_ASPECT_RATIO = 0.8f

@Preview
@Composable
private fun SparrowCodeFieldPreview() {
    SparrowTheme {
        SparrowCodeField(value = "1234", onValueChange = {})
    }
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
