package com.cbgm.sparrow.navigation.routing

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

internal fun AnimatedContentTransitionScope<*>.slideInFromRight(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

internal fun AnimatedContentTransitionScope<*>.slideInFromLeft(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

internal fun AnimatedContentTransitionScope<*>.slideOutToLeft(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

internal fun AnimatedContentTransitionScope<*>.slideOutToRight(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
