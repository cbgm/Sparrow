package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.warning
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveRouteProtectionUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.RouteProtection
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_route_protection_direct
import com.cbgm.sparrow.resources.feature_chats_route_protection_ok
import com.cbgm.sparrow.resources.feature_chats_route_protection_reduced
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The shield in the top bar of the chat list and of every chat: quiet with a check while the route protects as chosen,
 * a warning with an exclamation mark while it protects less, an error while requests go out directly. It opens Onion
 * routing, which tells why.
 */
@Composable
fun RouteProtectionAction() {
    // Previews have no Koin: they show the quiet shield.
    if (LocalInspectionMode.current) {
        RouteProtectionIcon(protection = RouteProtection.PROTECTED, onClick = {})
        return
    }
    val viewModel: RouteProtectionViewModel = koinViewModel()
    val protection by viewModel.protection.collectAsStateWithLifecycle()
    RouteProtectionIcon(protection = protection, onClick = viewModel::onClicked)
}

@Composable
private fun RouteProtectionIcon(
    protection: RouteProtection,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = protection.icon,
            contentDescription = stringResource(protection.description),
            tint = protection.tint(),
            modifier = Modifier.size(Dimens.TopBar.iconSize)
        )
    }
}

class RouteProtectionViewModel(
    observeRouteProtection: ObserveRouteProtectionUseCase
) : BaseViewModel() {
    val protection: StateFlow<RouteProtection> =
        observeRouteProtection().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = RouteProtection.PROTECTED
        )

    fun onClicked() {
        navigator.navigateTo(AppRoute.CircuitHops)
    }
}

private val RouteProtection.icon: ImageVector
    get() =
        when (this) {
            RouteProtection.PROTECTED -> SparrowIcons.ShieldCheck
            RouteProtection.REDUCED, RouteProtection.DIRECT -> SparrowIcons.ShieldAlert
        }

private val RouteProtection.description
    get() =
        when (this) {
            RouteProtection.PROTECTED -> Res.string.feature_chats_route_protection_ok
            RouteProtection.REDUCED -> Res.string.feature_chats_route_protection_reduced
            RouteProtection.DIRECT -> Res.string.feature_chats_route_protection_direct
        }

@Composable
private fun RouteProtection.tint(): Color =
    when (this) {
        RouteProtection.PROTECTED -> MaterialTheme.colorScheme.onSurfaceVariant
        RouteProtection.REDUCED -> MaterialTheme.colorScheme.warning
        RouteProtection.DIRECT -> MaterialTheme.colorScheme.error
    }
