package dev.jurczyk.renoplan.domain.model

import kotlin.uuid.Uuid

/** One floor of an apartment. */
data class Level(
    val id: Uuid,
    val name: String,
    val rooms: List<Room> = emptyList(),
)
