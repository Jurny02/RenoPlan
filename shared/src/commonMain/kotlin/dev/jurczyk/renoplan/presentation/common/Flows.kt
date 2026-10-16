package dev.jurczyk.renoplan.presentation.common

import kotlinx.coroutines.flow.SharingStarted

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Keeps upstream flows alive across configuration changes, stops them shortly after the screen leaves. */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS)
