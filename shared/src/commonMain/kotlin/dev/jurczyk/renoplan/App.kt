package dev.jurczyk.renoplan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jurczyk.renoplan.ui.navigation.RenoPlanNavigation
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme

@Composable
fun App(modifier: Modifier = Modifier) {
    RenoPlanTheme {
        RenoPlanNavigation(modifier = modifier)
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
