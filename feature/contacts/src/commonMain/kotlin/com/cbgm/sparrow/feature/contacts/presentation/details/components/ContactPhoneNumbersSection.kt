package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_phone_numbers
import com.cbgm.sparrow.resources.feature_contacts_no_phone_numbers_stored
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactPhoneNumbersSection(
    phoneNumbers: List<ContactPhoneNumber>,
    preferredPhoneNumberId: String?
) {
    SectionTitle(
        icon = Icons.Default.Phone,
        title = stringResource(Res.string.base_phone_numbers)
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    if (phoneNumbers.isEmpty()) {
        Text(
            text = stringResource(Res.string.feature_contacts_no_phone_numbers_stored),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base.div(2))) {
            phoneNumbers.forEach { phoneNumber ->
                ContactPhoneNumberRow(
                    phoneNumber = phoneNumber,
                    isPreferred = phoneNumber.id == preferredPhoneNumberId
                )
            }
        }
    }
}

@Preview
@Composable
private fun ContactPhoneNumbersSectionPreview() {
    SparrowTheme {
        ContactPhoneNumbersSection(
            phoneNumbers = listOf(ContactDetailsPreviewData.phoneNumber),
            preferredPhoneNumberId = ContactDetailsPreviewData.phoneNumber.id
        )
    }
}
