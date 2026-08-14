package com.cbgm.securechat.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.feature.settings.presentation.developer.DeveloperMenuRoute
import com.cbgm.securechat.feature.settings.presentation.disclaimer.DisclaimerRoute
import com.cbgm.securechat.feature.settings.presentation.disclaimer.model.DisclaimerType
import com.cbgm.securechat.feature.settings.presentation.licenses.LicensesRoute
import com.cbgm.securechat.feature.settings.presentation.network.ControlPlaneSettingsRoute
import com.cbgm.securechat.navigation.slideInFromRight
import com.cbgm.securechat.navigation.slideOutToRight

fun NavGraphBuilder.settingsNavGraph() {
    composable<AppRoute.PrivacyPolicy>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        DisclaimerRoute(type = DisclaimerType.PRIVACY_POLICY)
    }

    composable<AppRoute.DataDisclaimer>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        DisclaimerRoute(type = DisclaimerType.DATA_DISCLAIMER)
    }

    composable<AppRoute.Licenses>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        LicensesRoute()
    }

    composable<AppRoute.DeveloperMenu>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        DeveloperMenuRoute()
    }

    composable<AppRoute.ControlPlanes>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        ControlPlaneSettingsRoute()
    }
}
