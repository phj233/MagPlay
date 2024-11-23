package top.phj233.magplay

import androidx.compose.runtime.Composable
import top.phj233.magplay.nav.MagPlayNavHost
import top.phj233.magplay.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        MagPlayNavHost()
    }
}