package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarRing
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsContactUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contacts_no_sparrow_identity
import com.cbgm.sparrow.resources.feature_contacts_sparrow_contact_not_verified
import com.cbgm.sparrow.resources.feature_contacts_unnamed_contact
import com.cbgm.sparrow.resources.feature_contacts_verified_by_contact
import com.cbgm.sparrow.resources.feature_contacts_verified_by_you
import com.cbgm.sparrow.resources.feature_contacts_verified_sparrow_contact
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactHeader(contact: ContactDetailsContactUi) {
    val identity = contact.sparrowIdentity
    val verifiedByMe = identity?.verifiedByMe == true
    val verifiedByContact =
        identity?.mutualKeyExchange == true && identity.verifiedByContact
    val isMutuallyVerified = verifiedByMe && verifiedByContact

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MaterialTheme.spacing.large,
                bottom = MaterialTheme.spacing.medium
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SparrowAvatar(
            name = contact.displayName.orEmpty(),
            target = AvatarTarget.User(contact.id),
            size = Dimens.Avatar.profileSize,
            ring =
                when {
                    isMutuallyVerified -> AvatarRing.VERIFIED
                    identity != null -> AvatarRing.PENDING
                    else -> AvatarRing.NONE
                }
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = contact.displayName
                ?: stringResource(Res.string.feature_contacts_unnamed_contact),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        Text(
            text =
                when {
                    identity == null ->
                        stringResource(Res.string.feature_contacts_no_sparrow_identity)

                    isMutuallyVerified ->
                        stringResource(Res.string.feature_contacts_verified_sparrow_contact)

                    verifiedByMe ->
                        stringResource(Res.string.feature_contacts_verified_by_you)

                    verifiedByContact ->
                        stringResource(Res.string.feature_contacts_verified_by_contact)

                    else ->
                        stringResource(Res.string.feature_contacts_sparrow_contact_not_verified)
                },
            style = MaterialTheme.typography.bodySmall,
            color =
                when {
                    identity == null -> MaterialTheme.colorScheme.error
                    isMutuallyVerified -> MaterialTheme.colorScheme.tertiary
                    verifiedByMe -> MaterialTheme.colorScheme.primary
                    verifiedByContact -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
                },
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun ContactHeaderPreview() {
    SparrowTheme {
        ContactHeader(contact = ContactDetailsPreviewData.contact)
    }
}
