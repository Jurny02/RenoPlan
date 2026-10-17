package dev.jurczyk.renoplan.domain.model

/** Where an operation is applied. */
sealed interface Surface {
    data class Wall(
        val number: Int,
    ) : Surface

    data object Floor : Surface

    data object Ceiling : Surface

    /** Linear, measured in metres. */
    data object Skirting : Surface

    /** Linear, measured in metres. */
    data object CrownMoulding : Surface

    /**
     * A quantity typed by the user instead of measured, e.g. half a wall (m²) or crown moulding on one
     * wall (m). An operation has at most one.
     */
    data class Custom(
        val quantity: Double,
        val unit: MeasureUnit,
    ) : Surface
}
