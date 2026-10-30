package com.example.minilauncher

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Имя в приветствии. Поменяйте на своё.
private const val USER_NAME = "Владислав"

/**
 * Верхняя часть главного экрана: приветствие, часы и дату слева,
 * справа кнопки поиска и настроек и индикатор сети (только если сеть известна).
 */
@Composable
fun HomeTop(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    var network by remember { mutableStateOf(networkLabel(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            network = networkLabel(context)
            delay(15_000L)
        }
    }

    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }

    val locale = Locale.getDefault()
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", locale).format(now)
    val dateText = SimpleDateFormat("EEEE, d MMMM", locale)
        .format(now)
        .replaceFirstChar { it.uppercase() }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Tokens.SafeHorizontal,
                end = Tokens.SafeHorizontal,
                top = Tokens.SafeTop
            )
    ) {
        AnimatedVisibility(
            visible = appear,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(tween(durationMillis = 900)) +
                slideInVertically(initialOffsetY = { -it / 4 })
        ) {
            Column {
                Text(
                    text = (greeting(hour) + ", " + USER_NAME).uppercase(),
                    fontSize = 24.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium,
                    color = Tokens.TextSecondary
                )
                Text(
                    text = time,
                    fontSize = 132.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = (-2).sp,
                    color = Tokens.TextPrimary
                )
                Text(
                    text = dateText,
                    fontSize = 30.sp,
                    color = Tokens.TextSecondary
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "●  $network",
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            GlassButton(text = "Поиск", onClick = onSearch)
            GlassButton(text = "Настройки", onClick = onSettings)
        }
    }
}

private fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Доброе утро"
    in 12..16 -> "Добрый день"
    in 17..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

private fun networkLabel(context: Context): String = runCatching {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return@runCatching "Нет сети"
    when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Сеть"
    }
}.getOrDefault("Сеть")
