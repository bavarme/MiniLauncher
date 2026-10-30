package com.example.minilauncher

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Обои из res/drawable (wall_01 … wall_12). Лежат в приложении, интернет не нужен. */
internal val WALLPAPERS = listOf(
    R.drawable.wall_01, R.drawable.wall_02, R.drawable.wall_03, R.drawable.wall_04,
    R.drawable.wall_05, R.drawable.wall_06, R.drawable.wall_07, R.drawable.wall_08,
    R.drawable.wall_09, R.drawable.wall_10, R.drawable.wall_11, R.drawable.wall_12
)

/**
 * Фон главного экрана: обои с плавной сменой (crossfade), затемнение сверху и снизу,
 * и подсветка снизу цветом выбранного приложения.
 */
@Composable
fun HomeBackground(wallIndex: Int, accent: Color, reducedMotion: Boolean) {
    Crossfade(
        targetState = wallIndex,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else Tokens.MS_CROSSFADE),
        label = "wallpaper"
    ) { index ->
        Wallpaper(res = WALLPAPERS[index], slow = !reducedMotion)
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

/**
 * Одна картинка с медленным приближением. Анимация одноразовая (не бесконечная),
 * поэтому на слабом ТВ она почти ничего не стоит. При reduced motion отключена.
 */
@Composable
internal fun Wallpaper(res: Int, slow: Boolean) {
    val zoom = remember { Animatable(1f) }
    LaunchedEffect(slow) {
        if (slow) zoom.animateTo(1.05f, tween(durationMillis = 32_000, easing = LinearEasing))
    }
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = zoom.value
                scaleY = zoom.value
            }
    )
}
