package dev.jurczyk.renoplan.ui.navigation

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.ic_calculator
import renoplan.shared.generated.resources.ic_project
import renoplan.shared.generated.resources.ic_templates
import renoplan.shared.generated.resources.nav_calculator
import renoplan.shared.generated.resources.nav_project
import renoplan.shared.generated.resources.nav_templates

/** Destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    val label: StringResource,
    val icon: DrawableResource,
) {
    PROJECT(ProjectGraph, Res.string.nav_project, Res.drawable.ic_project),
    TEMPLATES(TemplatesRoute, Res.string.nav_templates, Res.drawable.ic_templates),
    CALCULATOR(CalculatorRoute, Res.string.nav_calculator, Res.drawable.ic_calculator),
}
