package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemePreset(val displayName: String) {
    NEON_OBSIDIAN("Neon Obsidian"),
    CYBER_AMETHYST("Cyber Amethyst"),
    PURE_VOID("Pure Void Black"),
    CRYSTAL_LIGHT("Crystal Light")
}

@Immutable
data class AiThemeTokens(
    val glassSurface: Color,
    val glassBorder: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val codeBackground: Color,
    val isDark: Boolean,
    val glowMultiplier: Float
)

val LocalAiThemeTokens = staticCompositionLocalOf {
    AiThemeTokens(
        glassSurface = GlassSurfaceDark,
        glassBorder = GlassBorderDark,
        glowPrimary = NeonPurple,
        glowSecondary = ElectricViolet,
        codeBackground = Color(0xFF0A0812),
        isDark = true,
        glowMultiplier = 1.0f
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun AiAssistantTheme(
    preset: ThemePreset = ThemePreset.NEON_OBSIDIAN,
    glowMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val isDark = preset != ThemePreset.CRYSTAL_LIGHT
    val primaryAccent = when (preset) {
        ThemePreset.NEON_OBSIDIAN -> NeonPurple
        ThemePreset.CYBER_AMETHYST -> Color(0xFF8B5CF6)
        ThemePreset.PURE_VOID -> ElectricViolet
        ThemePreset.CRYSTAL_LIGHT -> Color(0xFF7B2CBF)
    }

    val colorScheme = if (isDark) {
        val bg = when (preset) {
            ThemePreset.PURE_VOID -> Color(0xFF000000)
            ThemePreset.CYBER_AMETHYST -> Color(0xFF090714)
            else -> ObsidianBlack
        }
        val surface = when (preset) {
            ThemePreset.PURE_VOID -> Color(0xFF08070D)
            ThemePreset.CYBER_AMETHYST -> Color(0xFF120D24)
            else -> ObsidianSurface
        }
        val container = when (preset) {
            ThemePreset.PURE_VOID -> Color(0xFF100E18)
            ThemePreset.CYBER_AMETHYST -> Color(0xFF1B1434)
            else -> ObsidianCard
        }
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = DeepAmethyst,
            onPrimaryContainer = SoftLavender,
            secondary = ElectricViolet,
            onSecondary = ObsidianBlack,
            secondaryContainer = Color(0xFF2B194D),
            onSecondaryContainer = SoftLavender,
            tertiary = CyberCyan,
            onTertiary = ObsidianBlack,
            background = bg,
            onBackground = TextPrimaryDark,
            surface = surface,
            onSurface = TextPrimaryDark,
            surfaceVariant = container,
            onSurfaceVariant = TextSecondaryDark,
            surfaceContainerLowest = bg,
            surfaceContainerLow = surface,
            surfaceContainer = container,
            surfaceContainerHigh = ObsidianCardElevated,
            outline = GlassBorderDark,
            outlineVariant = Color(0x26A855F7),
            error = CrimsonError,
            onError = ObsidianBlack
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEDE2FF),
            onPrimaryContainer = Color(0xFF2D0A5E),
            secondary = Color(0xFF9333EA),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFF3E8FF),
            onSecondaryContainer = Color(0xFF3B0764),
            tertiary = Color(0xFF0284C7),
            onTertiary = Color.White,
            background = LightSurfaceBg,
            onBackground = LightTextPrimary,
            surface = LightCardSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = Color(0xFFEDE9F8),
            onSurfaceVariant = LightTextSecondary,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF5F2FC),
            surfaceContainer = Color(0xFFEEEAF9),
            surfaceContainerHigh = Color(0xFFE4DEF5),
            outline = LightGlassBorder,
            outlineVariant = Color(0x227B2CBF),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }

    val tokens = AiThemeTokens(
        glassSurface = if (isDark) GlassSurfaceDark else Color(0xCCFFFFFF),
        glassBorder = if (isDark) primaryAccent.copy(alpha = 0.28f) else LightGlassBorder,
        glowPrimary = primaryAccent,
        glowSecondary = if (isDark) ElectricViolet else Color(0xFFA855F7),
        codeBackground = if (isDark) Color(0xFF090710) else Color(0xFF151221),
        isDark = isDark,
        glowMultiplier = glowMultiplier.coerceIn(0.4f, 1.5f)
    )

    CompositionLocalProvider(LocalAiThemeTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
