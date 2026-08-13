package com.cbgm.securechat.feature.settings.presentation.disclaimer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.securechat.feature.settings.domain.model.DisclaimerContent
import com.cbgm.securechat.feature.settings.presentation.disclaimer.model.DisclaimerType
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_settings_data_disclaimer
import com.cbgm.securechat.resources.feature_settings_privacy_policy
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DisclaimerRoute(
    type: DisclaimerType,
    modifier: Modifier = Modifier,
    viewModel: DisclaimerViewModel = koinViewModel()
) {
    MarkdownDisclaimerScreen(
        title =
            when (type) {
                DisclaimerType.PRIVACY_POLICY -> stringResource(Res.string.feature_settings_privacy_policy)
                DisclaimerType.DATA_DISCLAIMER -> stringResource(Res.string.feature_settings_data_disclaimer)
            },
        markdownContent =
            when (type) {
                DisclaimerType.PRIVACY_POLICY -> DisclaimerContent.privacyPolicy
                DisclaimerType.DATA_DISCLAIMER -> DisclaimerContent.dataDisclaimer
            },
        onUiEvent = viewModel::onUiEvent,
        modifier = modifier
    )
}
