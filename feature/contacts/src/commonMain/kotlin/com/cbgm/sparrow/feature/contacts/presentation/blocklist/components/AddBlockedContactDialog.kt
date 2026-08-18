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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_contacts_add_blocked_contact
import com.cbgm.sparrow.resources.feature_contacts_add_blocked_contact_description
import com.cbgm.sparrow.resources.feature_contacts_block_phone_number
import com.cbgm.sparrow.resources.feature_contacts_choose_existing_contact
import com.cbgm.sparrow.resources.feature_contacts_no_contacts_to_block
import com.cbgm.sparrow.resources.feature_contacts_no_phone_number
import com.cbgm.sparrow.resources.feature_contacts_phone_number
import com.cbgm.sparrow.resources.feature_contacts_sparrow_contact
import com.cbgm.sparrow.resources.feature_contacts_unnamed_contact
import org.jetbrains.compose.resources.stringResource

private val Field = Color(0xFF102A46)

@Composable
fun AddBlockedContactDialog(
    phoneNumber: String,
    phoneNumberError: String?,
    contacts: List<Contact>,
    profilePictures: Map<String, ByteArray?>,
    enabled: Boolean,
    onPhoneNumberChanged: (String) -> Unit,
    onBlockPhoneNumber: () -> Unit,
    onContactSelected: (Contact) -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_contacts_add_blocked_contact),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                Text(
                    text = stringResource(Res.string.feature_contacts_add_blocked_contact_description),
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true,
                    isError = phoneNumberError != null,
                    label = {
                        Text(text = stringResource(Res.string.feature_contacts_phone_number))
                    },
                    supportingText =
                        phoneNumberError?.let { error ->
                            {
                                Text(text = error)
                            }
                        },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = Field,
                            unfocusedContainerColor = Field,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.TextField.unfocusedBorder),
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
                            cursorColor = MaterialTheme.colorScheme.secondary
                        )
                )

                SparrowApprovalButton(
                    onClick = onBlockPhoneNumber,
                    fillMaxWidth = false,
                    enabled = enabled && phoneNumber.isNotBlank(),
                    text = stringResource(Res.string.feature_contacts_block_phone_number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(Res.string.feature_contacts_choose_existing_contact),
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
                        style = MaterialTheme.typography.labelMedium
                    )

                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                if (contacts.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.feature_contacts_no_contacts_to_block),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = Dimens.BlockedContactsScreen.dialogListMaxHeight)) {
                        items(
                            items = contacts,
                            key = Contact::id
                        ) { contact ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = enabled) {
                                            onContactSelected(contact)
                                        }.padding(vertical = MaterialTheme.spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SparrowAvatar(
                                    name =
                                        contact.displayName
                                            ?: contact.preferredPhoneNumber?.value ?: "?",
                                    pictureBytes = profilePictures[contact.id]
                                )
                                Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text =
                                            contact.displayName
                                                ?: contact.preferredPhoneNumber?.value
                                                ?: stringResource(Res.string.feature_contacts_unnamed_contact),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = contact.subtitle(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowSecondaryButton(
                fillMaxWidth = false,
                onClick = onDismiss,
                text = stringResource(Res.string.base_close)
            )
        }
    )
}

@Composable
private fun Contact.subtitle(): String =
    preferredPhoneNumber?.value
        ?: if (sparrowIdentity != null) {
            stringResource(Res.string.feature_contacts_sparrow_contact)
        } else {
            stringResource(Res.string.feature_contacts_no_phone_number)
        }
