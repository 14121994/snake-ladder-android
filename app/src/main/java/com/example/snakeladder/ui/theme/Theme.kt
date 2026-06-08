package com.example.snakeladder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = MdThemeLightPrimary,
    onPrimary = MdThemeLightOnPrimary,
    background = MdThemeLightBackground,
    onBackground = MdThemeLightOnBackground
)

@Composable
fun SnakeLadderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
