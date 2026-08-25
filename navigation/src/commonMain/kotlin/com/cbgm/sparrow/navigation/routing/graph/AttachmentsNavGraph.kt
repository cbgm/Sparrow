package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.attachments.presentation.management.AttachmentManagementRoute
import com.cbgm.sparrow.feature.attachments.presentation.storage.AttachmentStorageRoute
import com.cbgm.sparrow.navigation.routing.slideInFromRight
import com.cbgm.sparrow.navigation.routing.slideOutToRight

fun NavGraphBuilder.attachmentsNavGraph() {
    composable<AppRoute.AttachmentStorage>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        AttachmentStorageRoute()
    }

    composable<AppRoute.AttachmentManagement>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        AttachmentManagementRoute()
    }
}
