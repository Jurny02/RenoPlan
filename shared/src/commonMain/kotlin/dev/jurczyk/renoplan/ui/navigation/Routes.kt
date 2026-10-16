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

/** Creates a room when [roomId] is `null`, otherwise edits it. */
@Serializable
data class RoomEditorRoute(
    val levelId: String,
    val roomId: String? = null,
)

@Serializable
data object TemplatesRoute

@Serializable
data object CalculatorRoute
