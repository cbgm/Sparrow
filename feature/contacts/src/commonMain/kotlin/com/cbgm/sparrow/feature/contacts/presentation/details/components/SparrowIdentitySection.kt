package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowSectionLabel
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.presentation.details.model.SparrowIdentityUi
import com.cbgm.sparrow.feature.identity.presentation.component.SparrowKeyBlock
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_not_verified
import com.cbgm.sparrow.resources.feature_contacts_compare_before_trusting
import com.cbgm.sparrow.resources.feature_contacts_sparrow_identity
import com.cbgm.sparrow.resources.feature_contacts_verified_by_contact
import com.cbgm.sparrow.resources.feature_contacts_verified_by_contact_description
import com.cbgm.sparrow.resources.feature_contacts_verified_by_you
import com.cbgm.sparrow.resources.feature_contacts_verified_by_you_description
import com.cbgm.sparrow.resources.feature_contacts_verify_safety_number
import com.cbgm.sparrow.resources.feature_identity_encryption_public_key
import com.cbgm.sparrow.resources.feature_identity_encryption_public_key_description
import com.cbgm.sparrow.resources.feature_identity_signing_public_key
import com.cbgm.sparrow.resources.feature_identity_signing_public_key_description
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SparrowIdentitySection(
    identity: SparrowIdentityUi,
    safetyNumber: String?,
    onVerifyIdentity: () -> Unit,
    showTitle: Boolean = true
) {
    if (showTitle) {
        SparrowSectionLabel(text = stringResource(Res.string.feature_contacts_sparrow_identity))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
    }

    val verifiedByContact =
        identity.mutualKeyExchange && identity.verifiedByContact

    when {
        identity.verifiedByMe && verifiedByContact -> Unit

        identity.verifiedByMe ->
            ContactStatusRow(
                dotColor = MaterialTheme.colorScheme.primary,
                title = stringResource(Res.string.feature_contacts_verified_by_you),
                titleColor = MaterialTheme.colorScheme.onSurface,
                description = stringResource(Res.string.feature_contacts_verified_by_you_description)
            )

        verifiedByContact ->
            ContactStatusRow(
                dotColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(Res.string.feature_contacts_verified_by_contact),
                titleColor = MaterialTheme.colorScheme.onSurface,
                description = stringResource(Res.string.feature_contacts_verified_by_contact_description)
            )

        else ->
            ContactStatusRow(
                dotColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(Res.string.base_not_verified),
                titleColor = MaterialTheme.colorScheme.onSurface,
                description = stringResource(Res.string.feature_contacts_compare_before_trusting)
            )
    }

    if (!identity.verifiedByMe || !verifiedByContact) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }

    if (safetyNumber != null) {
        if (!identity.verifiedByMe) {
            SparrowApprovalButton(
                onClick = onVerifyIdentity,
                fillMaxWidth = false,
                text = stringResource(Res.string.feature_contacts_verify_safety_number)
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }

    SparrowKeyBlock(
        label = stringResource(Res.string.feature_identity_signing_public_key),
        description = stringResource(Res.string.feature_identity_signing_public_key_description),
        key = identity.signingFingerprint
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
    SparrowKeyBlock(
        label = stringResource(Res.string.feature_identity_encryption_public_key),
        description = stringResource(Res.string.feature_identity_encryption_public_key_description),
        key = identity.encryptionFingerprint
    )
}

@Preview
@Composable
private fun SparrowIdentitySectionPreview() {
    SparrowTheme {
        SparrowIdentitySection(
            identity = ContactDetailsPreviewData.identity,
            safetyNumber = ContactDetailsPreviewData.safetyNumber,
            onVerifyIdentity = {}
        )
    }
}
