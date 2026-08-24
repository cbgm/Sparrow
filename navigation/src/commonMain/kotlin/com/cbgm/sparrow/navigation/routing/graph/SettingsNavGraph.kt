package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.settings.presentation.developer.DeveloperMenuRoute
import com.cbgm.sparrow.feature.settings.presentation.disclaimer.DisclaimerRoute
import com.cbgm.sparrow.feature.settings.presentation.disclaimer.model.DisclaimerType
import com.cbgm.sparrow.feature.settings.presentation.licenses.LicensesRoute
import com.cbgm.sparrow.feature.settings.presentation.network.ControlPlaneSettingsRoute
import com.cbgm.sparrow.feature.settings.presentation.profile.ProfileSettingsRoute
import com.cbgm.sparrow.navigation.routing.slideInFromRight
import com.cbgm.sparrow.navigation.routing.slideOutToRight

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

    composable<AppRoute.ProfileSettings>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        ProfileSettingsRoute()
    }

    composable<AppRoute.ControlPlanes>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        ControlPlaneSettingsRoute()
    }
}
