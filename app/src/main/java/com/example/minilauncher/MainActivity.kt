package com.example.minilauncher

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.KeyEvent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.platform.LocalContext
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

    // Открыт ли экран настроек. Кнопка «Меню» на пульте переключает его.
    val settingsOpen = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherScreen(settingsOpen)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            settingsOpen.value = !settingsOpen.value
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun LauncherScreen(settingsOpen: MutableState<Boolean>) {
    val context = LocalContext.current
    var open by settingsOpen

    // «Назад» закрывает настройки и ничего больше не делает
    BackHandler(enabled = true) {
        if (open) open = false
    }

    // Смена обоев каждые 30 секунд
    var wallIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(WALLPAPER_INTERVAL_MS)
            wallIndex = (wallIndex + 1) % WALLPAPERS.size
        }
    }

    // Все приложения собираем в фоне
    val allApps by produceState(initialValue = emptyList<AppData>(), context) {
        value = withContext(Dispatchers.IO) { AppRepository.loadUserApps(context) }
    }

    // Сохранённый порядок и скрытые приложения
    var order by remember { mutableStateOf<List<String>>(emptyList()) }
    var hidden by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) {
        order = AppPrefs.order(context)
        hidden = AppPrefs.hidden(context)
    }

    val arranged = remember(allApps, order) { AppPrefs.arrange(allApps, order) }
    val visible = arranged.filter { it.packageName !in hidden }

    fun save(newOrder: List<String>, newHidden: Set<String>) {
        order = newOrder
        hidden = newHidden
        AppPrefs.save(context, newOrder, newHidden)
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

        if (!open) {
            Column(modifier = Modifier.fillMaxSize()) {
                HeroSection(
                    modifier = Modifier.weight(0.6f),
                    onOpenSettings = { open = true }
                )
                DockSection(apps = visible, modifier = Modifier.weight(0.4f))
            }
        } else {
            SettingsScreen(
                apps = arranged,
                hidden = hidden,
                onMove = { from, to ->
                    val list = arranged.map { it.packageName }.toMutableList()
                    val item = list.removeAt(from)
                    list.add(to, item)
                    save(list, hidden)
                },
                onToggle = { pkg ->
                    val h = hidden.toMutableSet()
                    if (!h.add(pkg)) h.remove(pkg)
                    save(arranged.map { it.packageName }, h)
                },
                onClose = { open = false }
            )
        }
    }
}

// ---------------------------------------------------------------- верхняя часть (60%)

@Composable
private fun HeroSection(modifier: Modifier = Modifier, onOpenSettings: () -> Unit) {
    val context = LocalContext.current

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
        // Левый верхний угол: приветствие, часы, дата
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

        // Правый верхний угол: настройки, погода, сеть
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsButton(text = "⚙  Настроить", onClick = onOpenSettings)
            // Погода: нужен сервис погоды и ключ, поэтому пока заглушка
            Text(text = "☀ --°", fontSize = 28.sp, color = Color.White)
            Text(
                text = "● " + networkLabel(context),
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
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
    val context = LocalContext.current
    var focusedIndex by remember { mutableIntStateOf(-1) }
    val firstFocus = remember { FocusRequester() }

    // Фокус на первую иконку, чтобы пульт сразу работал
    LaunchedEffect(apps.size) {
        if (apps.isNotEmpty()) runCatching { firstFocus.requestFocus() }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (apps.isEmpty()) {
            Text(
                text = "Нет приложений. Нажмите «Меню» на пульте, чтобы открыть настройки.",
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
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

// ---------------------------------------------------------------- настройки

@Composable
private fun SettingsScreen(
    apps: List<AppData>,
    hidden: Set<String>,
    onMove: (from: Int, to: Int) -> Unit,
    onToggle: (packageName: String) -> Unit,
    onClose: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2000000))
            .padding(horizontal = 120.dp, vertical = 80.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Настройки лаунчера",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                SettingsButton(text = "Закрыть", onClick = onClose)
            }
            Spacer(modifier = Modifier.height(32.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    val isHidden = app.packageName in hidden
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = app.label,
                            fontSize = 28.sp,
                            color = if (isHidden) Color.White.copy(alpha = 0.4f) else Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        SettingsButton(
                            text = if (isHidden) "Показать" else "Скрыть",
                            onClick = { onToggle(app.packageName) },
                            focusRequester = if (index == 0) firstFocus else null
                        )
                        SettingsButton(
                            text = "▲",
                            enabled = index > 0,
                            onClick = { onMove(index, index - 1) }
                        )
                        SettingsButton(
                            text = "▼",
                            enabled = index < apps.size - 1,
                            onClick = { onMove(index, index + 1) }
                        )
                    }
                }
            }
        }
    }
}

/** Кнопка с белой подсветкой при фокусе. */
@Composable
private fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(64.dp)
            .widthIn(min = 64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (focused) Color.White else Color(0x33FFFFFF))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            color = when {
                focused -> Color.Black
                enabled -> Color.White
                else -> Color.White.copy(alpha = 0.3f)
            }
        )
    }
}
