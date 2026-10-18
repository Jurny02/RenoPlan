package dev.jurczyk.renoplan.ui.navigation

import kotlinx.serialization.Serializable

/** The Project tab: levels, a level and its rooms. */
@Serializable
data object ProjectGraph

@Serializable
data object LevelsRoute

/** Ids are passed as strings; screens parse them back into `Uuid`. */
@Serializable
data class LevelRoute(
    val levelId: String,
)

@Serializable
data class RoomDetailsRoute(
    val levelId: String,
    val roomId: String,
)

/** Creates a room when [roomId] is `null`, otherwise edits it. */
@Serializable
data class RoomEditorRoute(
    val levelId: String,
    val roomId: String? = null,
)

/** Summary (quantities only) of one room. */
@Serializable
data class RoomSummaryRoute(
    val levelId: String,
    val roomId: String,
)

/** Adds an operation when [operationId] is `null`, otherwise edits it. */
@Serializable
data class OperationEditorRoute(
    val levelId: String,
    val roomId: String,
    val operationId: String? = null,
)

/** The Templates tab: the list and the template editor. */
@Serializable
data object TemplatesGraph

@Serializable
data object TemplatesRoute

/** Creates a template when [templateId] is `null`, otherwise edits it. */
@Serializable
data class TemplateEditorRoute(
    val templateId: String? = null,
)

@Serializable
data object CalculatorRoute
