package com.example.minilauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Все размеры заданы для ширины 1920 dp (4K при плотности 2). На других экранах масштаб пропорциональный. */
private const val DESIGN_WIDTH_DP = 1920f

@Composable
fun DesignScale(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val base = LocalDensity.current
    val scale = (configuration.screenWidthDp / DESIGN_WIDTH_DP).coerceIn(0.5f, 3f)
    CompositionLocalProvider(
        LocalDensity provides Density(base.density * scale, base.fontScale)
    ) {
        content()
    }
}

/** Токены дизайна: цвета, отступы, размеры и длительности анимаций в одном месте. */
object Tokens {
    val TextPrimary: Color = Color.White
    val TextSecondary: Color = Color(0xCCFFFFFF)
    val TextTertiary: Color = Color(0x80FFFFFF)
    val Glass: Color = Color(0x33FFFFFF)
    val GlassSelected: Color = Color(0x66FFFFFF)
    val GlassRow: Color = Color(0x14FFFFFF)
    val Scrim: Color = Color(0xE6000000)

    val SafeHorizontal: Dp = 96.dp
    val SafeTop: Dp = 72.dp
    val SafeBottom: Dp = 40.dp
    val RowGap: Dp = 40.dp
    val ButtonHeight: Dp = 72.dp
    val TileSize: Dp = 240.dp
    val TileRadius: Dp = 56.dp

    const val MS_FOCUS = 220
    const val MS_PANEL = 260
    const val MS_CROSSFADE = 1800
}
