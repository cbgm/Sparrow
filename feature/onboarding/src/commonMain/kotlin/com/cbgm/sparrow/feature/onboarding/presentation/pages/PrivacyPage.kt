package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.ListingRow
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_continue_action
import com.cbgm.sparrow.resources.feature_onboarding_contacts_stay_local
import com.cbgm.sparrow.resources.feature_onboarding_contacts_stay_local_description
import com.cbgm.sparrow.resources.feature_onboarding_e2ee_description
import com.cbgm.sparrow.resources.feature_onboarding_end_to_end_encryption
import com.cbgm.sparrow.resources.feature_onboarding_identity_belongs_to_you
import com.cbgm.sparrow.resources.feature_onboarding_identity_belongs_to_you_description
import com.cbgm.sparrow.resources.feature_onboarding_privacy_first
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivacyPage(onNext: () -> Unit) {
    Column(
        Modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.feature_onboarding_privacy_first),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        ListingRow(
            index = "01",
            title = stringResource(Res.string.feature_onboarding_end_to_end_encryption),
            description = stringResource(Res.string.feature_onboarding_e2ee_description)
        )
        ListingRow(
            index = "02",
            title = stringResource(Res.string.feature_onboarding_contacts_stay_local),
            description = stringResource(Res.string.feature_onboarding_contacts_stay_local_description)
        )
        ListingRow(
            index = "03",
            title = stringResource(Res.string.feature_onboarding_identity_belongs_to_you),
            description = stringResource(Res.string.feature_onboarding_identity_belongs_to_you_description)
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        SparrowApprovalButton(
            onClick = onNext,
            text = stringResource(Res.string.base_continue_action)
        )
    }
}

@Preview
@Composable
private fun PrivacyPagePreview() {
    SparrowTheme {
        PrivacyPage {}
    }
}
