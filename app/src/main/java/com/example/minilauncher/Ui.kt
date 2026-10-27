package com.example.minilauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Все размеры в приложении заданы для экрана 1920×1080 dp (это 4K при плотности 2).
 * На других ТВ интерфейс масштабируется пропорционально ширине экрана,
 * поэтому выглядит одинаково и на 4K, и на 1080p.
 */
private const val DESIGN_WIDTH_DP = 1920f

@Composable
fun DesignScale(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val base = LocalDensity.current
    val scale = (configuration.screenWidthDp / DESIGN_WIDTH_DP).coerceIn(0.5f, 3f)
    // Меняем плотность: все dp и sp растут или уменьшаются вместе
    CompositionLocalProvider(
        LocalDensity provides Density(base.density * scale, base.fontScale)
    ) {
        content()
    }
}
