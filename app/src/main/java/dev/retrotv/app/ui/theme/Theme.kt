package dev.retrotv.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val AppColorScheme = darkColorScheme()

@Composable
fun RetroGameTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content,
    )
}
