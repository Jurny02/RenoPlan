package dev.jurczyk.renoplan

import androidx.compose.ui.window.ComposeUIViewController

// Called from Swift as MainViewControllerKt.MainViewController(), so it keeps the PascalCase name.
@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }
