package com.example.minilauncher

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Заставка: обои из приложения по кругу с плавной сменой и, если включено, часы.
 * Показывается после простоя. Любая кнопка пульта её выключает (см. MainActivity).
 */
@Composable
fun ScreenSaver(wallSec: Int, showClock: Boolean, reducedMotion: Boolean) {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(wallSec) {
        while (true) {
            delay(wallSec * 1000L)
            index = (index + 1) % WALLPAPERS.size
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(
            targetState = index,
            animationSpec = tween(durationMillis = if (reducedMotion) 0 else Tokens.MS_CROSSFADE),
            label = "saverWallpaper"
        ) { i ->
            Wallpaper(res = WALLPAPERS[i], slow = !reducedMotion)
        }

        // Затемнение снизу, чтобы часы читались на любой картинке
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color(0xB3000000)
                    )
                )
        )

        if (showClock) SaverClock()
    }
}

@Composable
private fun SaverClock() {
    val context = LocalContext.current
    val is24 = remember { android.text.format.DateFormat.is24HourFormat(context) }

    // Часы показывают минуты, поэтому обновляем только на границе минуты
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        }
    }

    val locale = Locale.getDefault()
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", locale).format(now)
    val dateText = SimpleDateFormat("EEEE, d MMMM", locale)
        .format(now)
        .replaceFirstChar { it.uppercase() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Tokens.SafeHorizontal, vertical = Tokens.SafeTop),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Text(
                text = time,
                fontSize = 132.sp,
                fontWeight = FontWeight.Thin,
                color = Tokens.TextPrimary
            )
            Text(
                text = dateText,
                fontSize = 30.sp,
                color = Tokens.TextSecondary
            )
        }
    }
}
