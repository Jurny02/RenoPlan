package dev.jurczyk.renoplan.domain.model

/** Wall number [number] (1..n in outline order), followed by a 90° [turn]. */
data class Wall(
    val number: Int,
    val length: Millimetres,
    val turn: Turn,
    val openings: List<Opening> = emptyList(),
)

/** Direction of the 90° turn at the end of a wall. */
enum class Turn {
    LEFT,
    RIGHT,
}
