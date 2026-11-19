package com.cbgm.sparrow.feature.contacts.presentation.blocklist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowRadio
import com.cbgm.sparrow.core.ui.component.SparrowTabs
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_contact
import com.cbgm.sparrow.resources.feature_contacts_add_blocked_contact
import com.cbgm.sparrow.resources.feature_contacts_no_contacts_to_block
import com.cbgm.sparrow.resources.feature_contacts_no_phone_number
import com.cbgm.sparrow.resources.feature_contacts_phone_number
import com.cbgm.sparrow.resources.feature_contacts_sparrow_contact
import com.cbgm.sparrow.resources.feature_contacts_unnamed_contact
import com.cbgm.sparrow.resources.feature_identity_recovery_block_action
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddBlockedContactDialog(
    isVisible: Boolean,
    phoneNumber: String,
    phoneNumberError: String?,
    contacts: List<ContactUi>,
    enabled: Boolean,
    onPhoneNumberChanged: (String) -> Unit,
    onBlockPhoneNumber: () -> Unit,
    onContactSelected: (ContactUi) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedContact by remember { mutableStateOf<ContactUi?>(null) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            selectedTabIndex = 0
            selectedContact = null
        }
    }

    SparrowAlertDialog(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_contacts_add_blocked_contact),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                SparrowTabs(
                    labels = listOf(
                        stringResource(Res.string.feature_contacts_phone_number),
                        stringResource(Res.string.base_contact)
                    ),
                    selectedIndex = selectedTabIndex,
                    onSelect = { index ->
                        selectedTabIndex = index
                    },
                    isFullUnderLine = true
                )

                if (selectedTabIndex == 0) {
                    SparrowInputField(
                        value = phoneNumber,
                        onValueChange = onPhoneNumberChanged,
                        isEnabled = enabled,
                        isSingleLine = true,
                        isError = phoneNumberError != null,
                        label = stringResource(Res.string.feature_contacts_phone_number),
                        errorText = phoneNumberError ?: "",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        )
                    )
                } else {
                    if (contacts.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.feature_contacts_no_contacts_to_block),
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(max = Dimens.BlockedContactsScreen.dialogListMaxHeight),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            itemsIndexed(
                                items = contacts,
                                key = { _, contact -> contact.id }
                            ) { index, contact ->
                                val isSelected = selectedContact?.id == contact.id
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = enabled) {
                                                selectedContact = contact
                                            }.padding(MaterialTheme.spacing.medium),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SparrowAvatar(
                                        name =
                                            contact.displayName
                                                ?: contact.preferredPhoneNumber ?: "?",
                                        target = AvatarTarget.User(contact.id)
                                    )
                                    Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text =
                                                contact.displayName
                                                    ?: contact.preferredPhoneNumber
                                                    ?: stringResource(Res.string.feature_contacts_unnamed_contact),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = contact.subtitle(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
                                    SparrowRadio(
                                        selected = isSelected,
                                        onClick = null,
                                        enabled = enabled
                                    )
                                }
                                if (index < contacts.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = MaterialTheme.spacing.listDividerStart),
                                        thickness = Dimens.Base.dividerThickness,
                                        color = MaterialTheme.colorScheme.divider
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isConfirmEnabled = when (selectedTabIndex) {
                0 -> enabled && phoneNumber.isNotBlank()
                1 -> enabled && selectedContact != null
                else -> false
            }
            SparrowDialogAction(
                onClick = {
                    when (selectedTabIndex) {
                        0 -> onBlockPhoneNumber()
                        1 -> selectedContact?.let { onContactSelected(it) }
                    }
                },
                enabled = isConfirmEnabled,
                text = stringResource(Res.string.feature_identity_recovery_block_action),
                color = MaterialTheme.colorScheme.error
            )
        },
        dismissButton = {
            SparrowDialogAction(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun ContactUi.subtitle(): String =
    preferredPhoneNumber
        ?: if (hasSparrowIdentity) {
            stringResource(Res.string.feature_contacts_sparrow_contact)
        } else {
            stringResource(Res.string.feature_contacts_no_phone_number)
        }
