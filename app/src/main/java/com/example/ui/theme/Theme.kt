package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CandlelightGold,
    secondary = AshSilt,
    tertiary = RosePinkText,
    background = MidnightCharcoal,
    surface = EveningWarmInk,
    onPrimary = MidnightCharcoal,
    onSecondary = MidnightCharcoal,
    onTertiary = MidnightCharcoal,
    onBackground = HolyCreamText,
    onSurface = HolyCreamText,
    outline = DarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = ClayBronzePrivate,
    secondary = ClayBronzeDark,
    tertiary = CrimsonRed,
    background = LightParchment,
    surface = WarmParchmentCard,
    onPrimary = LightParchment,
    onSecondary = LightParchment,
    onTertiary = LightParchment,
    onBackground = SlateCoal,
    onSurface = SlateCoal,
    outline = DividerGold
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We default to false so that our highly tailored parchment design is strictly conserved
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
