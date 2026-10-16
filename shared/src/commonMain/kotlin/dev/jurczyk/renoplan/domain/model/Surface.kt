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
}
