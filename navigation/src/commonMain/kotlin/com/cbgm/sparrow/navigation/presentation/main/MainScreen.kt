package com.cbgm.sparrow.navigation.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowFloatingButton
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.component.SparrowScrollStateType
import com.cbgm.sparrow.core.ui.component.SparrowTabbedScaffold
import com.cbgm.sparrow.core.ui.component.SparrowTabbedScrollStates
import com.cbgm.sparrow.core.ui.component.SparrowTabs
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.ContactsFlow
import com.cbgm.sparrow.feature.chats.presentation.component.RouteProtectionAction
import com.cbgm.sparrow.feature.chats.presentation.overview.OverviewRoute
import com.cbgm.sparrow.feature.identity.presentation.overview.IdentityOverviewRoute
import com.cbgm.sparrow.feature.settings.presentation.overview.SettingsRoute
import com.cbgm.sparrow.navigation.presentation.main.model.MainTab
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Suppress("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    invitationCount: Int,
    isMessageSearchAvailable: Boolean,
    onOpenSearch: () -> Unit,
    onOpenInvitations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tabs = MainTab.entries

    val pagerState =
        rememberPagerState(
            initialPage = tabs.indexOf(MainTab.Chats),
            pageCount = { tabs.size }
        )

    val selectedTab = tabs[pagerState.currentPage]
    var showContactsOverlay by rememberSaveable { mutableStateOf(false) }

    val mainScrollTargets =
        mapOf(
            MainTab.Chats to SparrowScrollStateType.LazyList,
            MainTab.Me to SparrowScrollStateType.Scroll,
            MainTab.Settings to SparrowScrollStateType.Scroll
        )

    Box(modifier = modifier.fillMaxSize()) {
        SparrowTabbedScaffold(
            modifier = Modifier.fillMaxSize(),
            selectedScrollTarget = selectedTab,
            scrollTargets = mainScrollTargets,
            topBar = { containerColor ->
                MainTopBar(
                    selectedTab = selectedTab,
                    containerColor = containerColor,
                    invitationCount = invitationCount,
                    isMessageSearchAvailable = isMessageSearchAvailable,
                    onOpenSearch = onOpenSearch,
                    onOpenInvitations = onOpenInvitations
                )
            },
            floatingActionButton = {
                if (selectedTab == MainTab.Chats) {
                    SparrowFloatingButton(
                        icon = SparrowIcons.Edit,
                        onClick = { showContactsOverlay = true }
                    )
                }
            },
            bottomBar = { containerColor ->
                MainBottomBar(
                    selectedTab = selectedTab,
                    containerColor = containerColor,
                    onTabSelected = { tab ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page = tabs.indexOf(tab))
                        }
                    }
                )
            }
        ) { innerPadding, scrollStates ->
            MainContent(
                pagerState = pagerState,
                innerPadding = innerPadding,
                scrollStates = scrollStates
            )
        }

        SparrowOverlayHost(
            visible = showContactsOverlay,
            onDismissRequest = { showContactsOverlay = false },
            horizontalPadding = MaterialTheme.spacing.zero,
            topPadding = MaterialTheme.spacing.times(6)
        ) { dismissOverlay ->
            ContactsFlow(
                onDismiss = dismissOverlay,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    selectedTab: MainTab,
    containerColor: Color,
    invitationCount: Int,
    isMessageSearchAvailable: Boolean,
    onOpenSearch: () -> Unit,
    onOpenInvitations: () -> Unit
) {
    SparrowTopBar(
        title = {
            Text(
                text = stringResource(selectedTab.label)
            )
        },
        actions = {
            if (selectedTab == MainTab.Chats) {
                RouteProtectionAction()
                if (isMessageSearchAvailable) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                    ) {
                        Icon(
                            imageVector = SparrowIcons.Search,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.TopBar.iconSize)
                        )
                    }
                }
                IconButton(
                    onClick = onOpenInvitations,
                    enabled = invitationCount > 0,
                    modifier = Modifier
                ) {
                    BadgedBox(
                        badge = {
                            if (invitationCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(invitationCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = SparrowIcons.Mail,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.TopBar.iconSize)
                        )
                    }
                }
            }
        },
        containerColor = containerColor
    )
}

/** Text tabs: the open one is bright with a short cyan line under it. No icons, no line above. */
@Composable
private fun MainBottomBar(
    selectedTab: MainTab,
    containerColor: Color,
    onTabSelected: (MainTab) -> Unit
) {
    SparrowTabs(
        labels = MainTab.entries.map { tab -> stringResource(tab.label) },
        selectedIndex = MainTab.entries.indexOf(selectedTab),
        onSelect = { index -> onTabSelected(MainTab.entries[index]) },
        height = Dimens.MainScreen.bottomBarHeight,
        modifier = Modifier
            .background(containerColor)
            .navigationBarsPadding()
    )
}

@Composable
private fun MainContent(
    pagerState: PagerState,
    innerPadding: PaddingValues,
    scrollStates: SparrowTabbedScrollStates<MainTab>
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (MainTab.entries[page]) {
            MainTab.Chats -> {
                OverviewRoute(
                    listState = scrollStates.lazyListState(MainTab.Chats),
                    innerPadding = innerPadding,
                    modifier = Modifier.fillMaxSize()
                )
            }

            MainTab.Me -> {
                IdentityOverviewRoute(
                    scrollState = scrollStates.scrollState(MainTab.Me),
                    innerPadding = innerPadding,
                    modifier = Modifier.fillMaxSize()
                )
            }

            MainTab.Settings -> {
                SettingsRoute(
                    scrollState = scrollStates.scrollState(MainTab.Settings),
                    innerPadding = innerPadding
                )
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    SparrowTheme {
        MainScreen(
            invitationCount = 0,
            isMessageSearchAvailable = true,
            onOpenSearch = {},
            onOpenInvitations = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
