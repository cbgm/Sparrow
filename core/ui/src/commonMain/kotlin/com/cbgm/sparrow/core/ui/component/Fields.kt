package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

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

    BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.SearchField.searchHeight)
            .focusRequester(focusRequester ?: FocusRequester.Cancel),
        textStyle =
            MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = searchQuery,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding =
                    PaddingValues(
                        horizontal = MaterialTheme.spacing.textField.horizontalPadding,
                        vertical = MaterialTheme.spacing.textField.verticalPadding
                    ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.SearchField.searchIconSize)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onClear()
                            },
                            modifier = Modifier.size(Dimens.SearchField.searchClearButtonSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.SearchField.searchIconSize)
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                shape = MaterialTheme.shapes.extraSmall,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor =
                            MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
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
    keyboardOptions: KeyboardOptions? = null
) {
    val defaultKeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Phone,
        imeAction = ImeAction.Done
    )

    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = isEnabled,
        singleLine = isSingleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = keyboardOptions ?: defaultKeyboardOptions,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = isEnabled,
                singleLine = isSingleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                isError = isError,
                label = {
                    Text(text = label)
                },
                placeholder = {
                    Text(text = placeholderText)
                },
                supportingText =
                    if (isError) {
                        {
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else {
                        null
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = Alpha.TextField.unfocusedBorder
                    ),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = Alpha.OpaqueText
                    ),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.TextField.placeholder),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.TextField.placeholder)
                ),
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.spacing.textField.horizontalPadding,
                    vertical = MaterialTheme.spacing.textField.verticalPadding
                )
            )
        }
    )
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
