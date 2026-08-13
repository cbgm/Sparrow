package com.cbgm.securechat.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatOverlayHost
import com.cbgm.securechat.core.ui.component.SecureChatScrollStateType
import com.cbgm.securechat.core.ui.component.SecureChatTabbedScaffold
import com.cbgm.securechat.core.ui.component.SecureChatTabbedScrollStates
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.chats.presentation.ContactsFlow
import com.cbgm.securechat.feature.chats.presentation.overview.OverviewRoute
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute
import com.cbgm.securechat.feature.settings.presentation.overview.SettingsRoute
import com.cbgm.securechat.presentation.model.MainTab
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_contacts_open_invitations
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Suppress("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    invitationCount: Int,
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
            MainTab.Chats to SecureChatScrollStateType.LazyList,
            MainTab.Me to SecureChatScrollStateType.Scroll,
            MainTab.Settings to SecureChatScrollStateType.Scroll
        )

    Box(modifier = modifier.fillMaxSize()) {
        SecureChatTabbedScaffold(
            modifier = Modifier.fillMaxSize(),
            selectedScrollTarget = selectedTab,
            scrollTargets = mainScrollTargets,
            topBar = { containerColor ->
                MainTopBar(
                    selectedTab = selectedTab,
                    containerColor = containerColor,
                    invitationCount = invitationCount,
                    onOpenInvitations = onOpenInvitations,
                    onAddChat = { showContactsOverlay = true }
                )
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

        SecureChatOverlayHost(
            visible = showContactsOverlay,
            onDismissRequest = { showContactsOverlay = false },
            horizontalPadding = 0.dp,
            topPadding = 48.dp
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
    onOpenInvitations: () -> Unit,
    onAddChat: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(selectedTab.label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            if (selectedTab == MainTab.Chats) {
                IconButton(
                    onClick = onOpenInvitations,
                    enabled = invitationCount > 0
                ) {
                    BadgedBox(
                        badge = {
                            if (invitationCount > 0) {
                                Badge {
                                    Text(invitationCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MarkEmailUnread,
                            contentDescription = stringResource(Res.string.feature_contacts_open_invitations)
                        )
                    }
                }
                IconButton(onClick = onAddChat) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = ""
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
    )
}

@Composable
private fun MainBottomBar(
    selectedTab: MainTab,
    containerColor: Color,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        painter = painterResource(if (isSelected) tab.res else tab.resOutlined),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun MainContent(
    pagerState: PagerState,
    innerPadding: PaddingValues,
    scrollStates: SecureChatTabbedScrollStates<MainTab>
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
                IdentityRoute(
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
    SecureChatTheme {
        MainScreen(
            invitationCount = 0,
            onOpenInvitations = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
