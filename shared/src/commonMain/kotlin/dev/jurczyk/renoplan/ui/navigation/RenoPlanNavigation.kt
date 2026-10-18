package dev.jurczyk.renoplan.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.jurczyk.renoplan.ui.calculator.CalculatorScreen
import dev.jurczyk.renoplan.ui.components.hideKeyboardOnTapOrDrag
import dev.jurczyk.renoplan.ui.level.LevelScreen
import dev.jurczyk.renoplan.ui.levels.LevelsScreen
import dev.jurczyk.renoplan.ui.operation.OperationEditorScreen
import dev.jurczyk.renoplan.ui.roomdetails.RoomDetailsScreen
import dev.jurczyk.renoplan.ui.roomdetails.RoomSummaryScreen
import dev.jurczyk.renoplan.ui.roomeditor.RoomEditorScreen
import dev.jurczyk.renoplan.ui.templates.TemplateEditorScreen
import dev.jurczyk.renoplan.ui.templates.TemplatesScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun RenoPlanNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = modifier.hideKeyboardOnTapOrDrag(focusManager),
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
            startDestination = ProjectGraph,
            // Screens have their own Scaffold; consuming the insets keeps them from padding twice.
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition,
        ) {
            projectGraph(navController)
            templatesGraph(navController)
            composable<CalculatorRoute> { CalculatorScreen() }
        }
    }
}

private fun NavGraphBuilder.projectGraph(navController: NavHostController) {
    navigation<ProjectGraph>(startDestination = LevelsRoute) {
        composable<LevelsRoute> {
            LevelsScreen(onOpenLevel = { navController.navigate(LevelRoute(it.toString())) })
        }
        composable<LevelRoute> { entry ->
            val route = entry.toRoute<LevelRoute>()
            LevelScreen(
                levelId = Uuid.parse(route.levelId),
                onBack = navController::popBackStack,
                onAddRoom = { navController.navigate(RoomEditorRoute(route.levelId)) },
                onOpenRoom = { navController.navigate(RoomDetailsRoute(route.levelId, it.toString())) },
                onOpenOperation = { roomId, operationId ->
                    navController.navigate(
                        OperationEditorRoute(route.levelId, roomId.toString(), operationId.toString()),
                    )
                },
            )
        }
        roomDestinations(navController)
        composable<OperationEditorRoute> { entry ->
            val route = entry.toRoute<OperationEditorRoute>()
            OperationEditorScreen(
                levelId = Uuid.parse(route.levelId),
                roomId = Uuid.parse(route.roomId),
                operationId = route.operationId?.let(Uuid::parse),
                onBack = navController::popBackStack,
            )
        }
        composable<RoomEditorRoute> { entry ->
            val route = entry.toRoute<RoomEditorRoute>()
            RoomEditorScreen(
                levelId = Uuid.parse(route.levelId),
                roomId = route.roomId?.let(Uuid::parse),
                onBack = navController::popBackStack,
            )
        }
    }
}

/** Room details and the screens opened from it. */
private fun NavGraphBuilder.roomDestinations(navController: NavHostController) {
    composable<RoomDetailsRoute> { entry ->
        val route = entry.toRoute<RoomDetailsRoute>()
        RoomDetailsScreen(
            levelId = Uuid.parse(route.levelId),
            roomId = Uuid.parse(route.roomId),
            onBack = navController::popBackStack,
            onEditShape = { navController.navigate(RoomEditorRoute(route.levelId, route.roomId)) },
            onOpenSummary = { navController.navigate(RoomSummaryRoute(route.levelId, route.roomId)) },
            onAddOperation = { navController.navigate(OperationEditorRoute(route.levelId, route.roomId)) },
            onEditOperation = {
                navController.navigate(
                    OperationEditorRoute(route.levelId, route.roomId, it.toString()),
                )
            },
        )
    }
    composable<RoomSummaryRoute> { entry ->
        val route = entry.toRoute<RoomSummaryRoute>()
        RoomSummaryScreen(
            levelId = Uuid.parse(route.levelId),
            roomId = Uuid.parse(route.roomId),
            onBack = navController::popBackStack,
        )
    }
}

private fun NavGraphBuilder.templatesGraph(navController: NavHostController) {
    navigation<TemplatesGraph>(startDestination = TemplatesRoute) {
        composable<TemplatesRoute> {
            TemplatesScreen(
                onAddTemplate = { navController.navigate(TemplateEditorRoute()) },
                onOpenTemplate = { navController.navigate(TemplateEditorRoute(it.toString())) },
            )
        }
        composable<TemplateEditorRoute> { entry ->
            TemplateEditorScreen(
                templateId = entry.toRoute<TemplateEditorRoute>().templateId?.let(Uuid::parse),
                onBack = navController::popBackStack,
            )
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
