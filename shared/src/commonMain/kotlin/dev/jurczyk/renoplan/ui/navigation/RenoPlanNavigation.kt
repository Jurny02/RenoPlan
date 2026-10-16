package dev.jurczyk.renoplan.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.jurczyk.renoplan.ui.components.PlaceholderScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.screen_calculator
import renoplan.shared.generated.resources.screen_levels
import renoplan.shared.generated.resources.screen_templates

@Composable
fun RenoPlanNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination.isOnTopLevel(destination),
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LevelsRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<LevelsRoute> {
                PlaceholderScreen(title = stringResource(Res.string.screen_levels))
            }
            composable<TemplatesRoute> {
                PlaceholderScreen(title = stringResource(Res.string.screen_templates))
            }
            composable<CalculatorRoute> {
                PlaceholderScreen(title = stringResource(Res.string.screen_calculator))
            }
        }
    }
}

private fun NavDestination?.isOnTopLevel(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/** Switches tabs keeping one back stack per tab, as recommended for bottom navigation. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
