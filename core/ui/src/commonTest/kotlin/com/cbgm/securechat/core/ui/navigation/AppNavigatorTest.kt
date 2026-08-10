package com.cbgm.securechat.core.ui.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppNavigatorTest {
    @Test
    fun fastDuplicateDestinationIsEmittedOnlyOnce() =
        runTest {
            val navigator = AppNavigator()
            val events = mutableListOf<AppNavigationEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                navigator.navigationEvents.collect { event -> events += event }
            }
            val route = AppRoute.GroupConversation(conversationId = "group-1")

            navigator.navigateTo(route)
            navigator.navigateTo(route)

            assertEquals(
                expected = listOf(AppNavigationEvent.NavigateTo(route = route)),
                actual = events
            )
        }

    @Test
    fun differentDestinationsAreNotSuppressed() =
        runTest {
            val navigator = AppNavigator()
            val events = mutableListOf<AppNavigationEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                navigator.navigationEvents.collect { event -> events += event }
            }
            val firstRoute = AppRoute.GroupConversation(conversationId = "group-1")
            val secondRoute = AppRoute.GroupConversation(conversationId = "group-2")

            navigator.navigateTo(firstRoute)
            navigator.navigateTo(secondRoute)

            assertEquals(
                expected =
                    listOf(
                        AppNavigationEvent.NavigateTo(route = firstRoute),
                        AppNavigationEvent.NavigateTo(route = secondRoute)
                    ),
                actual = events
            )
        }

    @Test
    fun backEventsAreNotDebounced() =
        runTest {
            val navigator = AppNavigator()
            val events = mutableListOf<AppNavigationEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                navigator.navigationEvents.collect { event -> events += event }
            }

            navigator.popBackStack()
            navigator.popBackStack()

            assertEquals(
                expected =
                    listOf(
                        AppNavigationEvent.PopBackStack(),
                        AppNavigationEvent.PopBackStack()
                    ),
                actual = events
            )
        }
}
