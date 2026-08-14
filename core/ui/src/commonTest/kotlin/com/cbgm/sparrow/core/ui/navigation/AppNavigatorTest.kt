package com.cbgm.sparrow.core.ui.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppNavigatorTest {
    @Test
    fun navigateToEmitsNavigationEvent() =
        runTest {
            val navigator = AppNavigator()
            val events = mutableListOf<AppNavigationEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                navigator.navigationEvents.collect { event -> events += event }
            }
            val route = AppRoute.GroupConversation(conversationId = "group-1")

            navigator.navigateTo(route)

            assertEquals(
                expected = listOf<AppNavigationEvent>(AppNavigationEvent.NavigateTo(route = route)),
                actual = events
            )
        }

    @Test
    fun popBackStackEmitsNavigationEvent() =
        runTest {
            val navigator = AppNavigator()
            val events = mutableListOf<AppNavigationEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                navigator.navigationEvents.collect { event -> events += event }
            }

            navigator.popBackStack()

            assertEquals(
                expected = listOf<AppNavigationEvent>(AppNavigationEvent.PopBackStack),
                actual = events
            )
        }
}
