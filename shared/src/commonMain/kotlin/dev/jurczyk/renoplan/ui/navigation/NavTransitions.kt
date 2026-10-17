package dev.jurczyk.renoplan.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

// Switching bottom-bar tabs is instant, like a native tab bar. Going deeper slides the new screen in
// from the end while the previous one shifts a little (iOS-style push); going back reverses it.

private const val PUSH_DURATION_MILLIS = 300

/** How far the screen underneath moves, as a fraction of the width. */
private const val PARALLAX_DIVISOR = 4

private typealias NavTransitionScope = AnimatedContentTransitionScope<NavBackStackEntry>

private fun <T> pushSpec() = tween<T>(durationMillis = PUSH_DURATION_MILLIS, easing = FastOutSlowInEasing)

private val NavDestination.topLevel: TopLevelDestination?
    get() = TopLevelDestination.entries.find { tab -> hierarchy.any { it.hasRoute(tab.route::class) } }

private val NavTransitionScope.isTabSwitch: Boolean
    get() = initialState.destination.topLevel != targetState.destination.topLevel

val enterTransition: NavTransitionScope.() -> EnterTransition = {
    if (isTabSwitch) EnterTransition.None else slideIntoContainer(SlideDirection.Start, pushSpec())
}

val exitTransition: NavTransitionScope.() -> ExitTransition = {
    if (isTabSwitch) {
        ExitTransition.None
    } else {
        slideOutOfContainer(SlideDirection.Start, pushSpec(), targetOffset = { it / PARALLAX_DIVISOR })
    }
}

val popEnterTransition: NavTransitionScope.() -> EnterTransition = {
    if (isTabSwitch) {
        EnterTransition.None
    } else {
        slideIntoContainer(SlideDirection.End, pushSpec(), initialOffset = { it / PARALLAX_DIVISOR })
    }
}

val popExitTransition: NavTransitionScope.() -> ExitTransition = {
    if (isTabSwitch) ExitTransition.None else slideOutOfContainer(SlideDirection.End, pushSpec())
}
