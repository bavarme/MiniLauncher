package com.example.minilauncher

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // Открыты ли настройки. Кнопка «Меню» на пульте переключает их.
    private val settingsOpen = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DesignScale {
                LauncherScreen(settingsOpen)
            }
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

    // «Назад» закрывает настройки и больше ничего не делает
    BackHandler(enabled = true) { open = false }

    var wallIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(WALLPAPER_INTERVAL_MS)
            wallIndex = (wallIndex + 1) % WALLPAPERS.size
        }
    }

    // Цвет подсветки: плавно меняется при фокусе на другой плитке
    var accentTarget by remember { mutableStateOf(Color(0xFF1C1C1E)) }
    val accent by animateColorAsState(
        targetValue = accentTarget,
        animationSpec = tween(durationMillis = 700),
        label = "accent"
    )

    val allApps by produceState(initialValue = emptyList<AppData>(), context) {
        value = withContext(Dispatchers.IO) { AppRepository.loadUserApps(context) }
    }

    // Порядок и скрытые приложения, сохранённые между запусками
    var order by remember { mutableStateOf<List<String>>(emptyList()) }
    var hidden by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) {
        order = AppPrefs.order(context)
        hidden = AppPrefs.hidden(context)
    }

    val arranged = remember(allApps, order) { AppPrefs.arrange(allApps, order) }
    val visible = remember(arranged, hidden) { arranged.filter { it.packageName !in hidden } }

    fun save(newOrder: List<String>, newHidden: Set<String>) {
        order = newOrder
        hidden = newHidden
        AppPrefs.save(context, newOrder, newHidden)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HomeBackground(wallIndex = wallIndex, accent = accent)

        if (!open) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeTop(
                    onOpenSettings = { open = true },
                    modifier = Modifier.weight(0.6f)
                )
                AppDock(
                    apps = visible,
                    onAccentChange = { accentTarget = it },
                    modifier = Modifier.weight(0.4f)
                )
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
