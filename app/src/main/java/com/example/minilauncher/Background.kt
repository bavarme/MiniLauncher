package com.example.minilauncher

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Как часто меняются обои. */
const val WALLPAPER_INTERVAL_MS = 30_000L

internal val WALLPAPERS = listOf(
    R.drawable.wall_01, R.drawable.wall_02, R.drawable.wall_03, R.drawable.wall_04,
    R.drawable.wall_05, R.drawable.wall_06, R.drawable.wall_07, R.drawable.wall_08,
    R.drawable.wall_09, R.drawable.wall_10, R.drawable.wall_11, R.drawable.wall_12
)

/**
 * Фон: обои с плавной сменой (crossfade) и медленным «дыханием» масштаба (Ken Burns).
 * Сверху и снизу затемнение, снизу дополнительно подсвечивается цветом выбранного приложения.
 */
@Composable
fun HomeBackground(wallIndex: Int, accent: Color) {
    val transition = rememberInfiniteTransition(label = "kenBurns")
    // Масштаб читается только при отрисовке, поэтому экран не перерисовывается целиком
    val zoom by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom"
    )

    Crossfade(
        targetState = wallIndex,
        animationSpec = tween(durationMillis = 2000),
        label = "wallpaper"
    ) { index ->
        Image(
            painter = painterResource(WALLPAPERS[index]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0x99000000),
                    0.35f to Color(0x14000000),
                    0.6f to Color(0x80000000),
                    1f to Color(0xF2000000)
                )
            )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.5f to Color.Transparent,
                    1f to accent.copy(alpha = 0.4f)
                )
            )
    )
}
