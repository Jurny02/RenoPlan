package dev.jurczyk.renoplan.ui.components

import androidx.compose.ui.platform.ClipEntry

/** A clipboard entry holding plain [text], for `LocalClipboard.current.setClipEntry(...)`. */
expect fun plainTextClipEntry(text: String): ClipEntry
