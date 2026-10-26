package com.example.minilauncher

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Имя в приветствии. Поменяйте на своё.
private const val USER_NAME = "Владислав"
private const val WALLPAPER_INTERVAL_MS = 30_000L

// Обои из res/drawable (wall_01 … wall_12)
private val WALLPAPERS = listOf(
    R.drawable.wall_01, R.drawable.wall_02, R.drawable.wall_03, R.drawable.wall_04,
    R.drawable.wall_05, R.drawable.wall_06, R.drawable.wall_07, R.drawable.wall_08,
    R.drawable.wall_09, R.drawable.wall_10, R.drawable.wall_11, R.drawable.wall_12
)

private val ICON_SIZE = 200.dp
private val ICON_CORNER = 40.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Домашний экран: «Назад» ничего не закрывает
            BackHandler(enabled = true) { }
            LauncherScreen()
        }
    }
}

@Composable
fun LauncherScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Смена обоев каждые 30 секунд
    var wallIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(WALLPAPER_INTERVAL_MS)
            wallIndex = (wallIndex + 1) % WALLPAPERS.size
        }
    }

    // Список приложений собираем в фоне
    val apps by produceState(initialValue = emptyList<AppData>(), context) {
        value = withContext(Dispatchers.IO) { AppRepository.loadUserApps(context) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Фон: обои с плавным переходом
        Crossfade(
            targetState = wallIndex,
            animationSpec = tween(durationMillis = 2000),
            label = "wallpaper"
        ) { index ->
            Image(
                painter = painterResource(WALLPAPERS[index]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Градиент снизу вверх, чтобы текст читался
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color(0x66000000),
                        1f to Color(0xEE000000)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            HeroSection(modifier = Modifier.weight(0.6f))
            DockSection(apps = apps, modifier = Modifier.weight(0.4f))
        }
    }
}

// ---------------------------------------------------------------- верхняя часть (60%)

@Composable
private fun HeroSection(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000L)
        }
    }

    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 96.dp, vertical = 72.dp)) {
        // Левый верхний угол: часы, дата, приветствие
        AnimatedVisibility(
            visible = appear,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(tween(900)) + slideInVertically(initialOffsetY = { -it / 4 })
        ) {
            Column {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                Text(
                    text = greeting(hour) + ", " + USER_NAME,
                    fontSize = 36.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.White
                )
                Text(
                    text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                        .format(now)
                        .replaceFirstChar { it.uppercase() },
                    fontSize = 26.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Правый верхний угол: погода и сеть
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Погода: нужен сервис погоды и ключ, поэтому пока заглушка
            Text(text = "☀ --°", fontSize = 28.sp, color = Color.White)
            Text(text = "● " + networkLabel(context), fontSize = 24.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

private fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Доброе утро"
    in 12..16 -> "Добрый день"
    in 17..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

private fun networkLabel(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "Нет сети"
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Сеть"
    }
}

// ---------------------------------------------------------------- нижняя панель (40%)

@Composable
private fun DockSection(apps: List<AppData>, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var focusedIndex by remember { mutableIntStateOf(-1) }
    val firstFocus = remember { FocusRequester() }

    // Фокус на первую иконку, чтобы пульт сразу работал
    LaunchedEffect(apps.size) {
        if (apps.isNotEmpty()) runCatching { firstFocus.requestFocus() }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (apps.isEmpty()) {
            Text(text = "Нет установленных приложений", fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    AppTile(
                        app = app,
                        index = index,
                        focusedIndex = focusedIndex,
                        focusRequester = if (index == 0) firstFocus else null,
                        onFocused = { focusedIndex = it },
                        onClick = {
                            runCatching { context.startActivity(app.launchIntent) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    app: AppData,
    index: Int,
    focusedIndex: Int,
    focusRequester: FocusRequester?,
    onFocused: (Int) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    // Фокус: иконка увеличивается
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.2f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "scale"
    )

    // Эффект «магнита»: соседние иконки отъезжают в стороны
    val shift by animateDpAsState(
        targetValue = when {
            focusedIndex < 0 || index == focusedIndex -> 0.dp
            index == focusedIndex - 1 -> (-24).dp
            index == focusedIndex + 1 -> 24.dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 250),
        label = "shift"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(x = shift)
    ) {
        Box(
            modifier = Modifier
                .size(ICON_SIZE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                // Мягкая тень при фокусе
                .shadow(elevation = if (focused) 32.dp else 0.dp, shape = RoundedCornerShape(ICON_CORNER))
                .clip(RoundedCornerShape(ICON_CORNER))
                .background(Color(0x33FFFFFF))
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { state ->
                    focused = state.isFocused
                    if (state.isFocused) onFocused(index)
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val icon = app.icon
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(28.dp)
                )
            } else {
                // Иконки нет: показываем звёздочку
                Text(text = "★", fontSize = 80.sp, color = Color.White)
            }
        }

        // Название под иконкой, только у выбранной
        AnimatedVisibility(visible = focused) {
            Text(
                text = app.label,
                fontSize = 26.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}
