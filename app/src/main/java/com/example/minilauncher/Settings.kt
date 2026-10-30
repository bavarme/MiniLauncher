package com.example.minilauncher

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TABS = listOf("Приложения", "Оформление", "Заставка", "Поиск", "Система")
private val WALL_STEPS = listOf(15, 30, 60, 300)
private val SS_STEPS = listOf(2, 5, 10, 30)

/** Настройки с вкладками. Каждое изменение сохраняется сразу. */
@Composable
fun SettingsScreen(
    state: LauncherState,
    apps: List<AppData>,
    versionLabel: String,
    onNextWallpaper: () -> Unit,
    onPreviewScreensaver: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onResetAll: () -> Unit,
    onClose: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val firstTab = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstTab.requestFocus() } }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.Scrim)
            .padding(horizontal = Tokens.SafeHorizontal, vertical = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(64.dp)
    ) {
        Column(
            modifier = Modifier.width(360.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Настройки",
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            TABS.forEachIndexed { i, title ->
                GlassButton(
                    text = title,
                    selected = tab == i,
                    onClick = { tab = i },
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = if (i == 0) firstTab else null
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            GlassButton(
                text = "Закрыть",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (tab) {
                0 -> AppsTab(state = state, apps = apps)
                1 -> AppearanceTab(state = state, onNextWallpaper = onNextWallpaper)
                2 -> ScreensaverTab(state = state, onPreview = onPreviewScreensaver)
                3 -> SearchTab(state = state)
                else -> SystemTab(
                    versionLabel = versionLabel,
                    onOpenAndroidSettings = onOpenAndroidSettings,
                    onResetAll = onResetAll
                )
            }
        }
    }
}

@Composable
private fun AppsTab(state: LauncherState, apps: List<AppData>) {
    val s = state.settings
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            val isHidden = app.packageName in s.hidden
            val isFav = app.packageName in s.favorites
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Tokens.GlassRow)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppThumb(app = app, width = 128.dp, height = 72.dp)
                Text(
                    text = app.label,
                    fontSize = 28.sp,
                    color = if (isHidden) Tokens.TextTertiary else Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = if (isFav) "Из избранного" else "В избранное",
                    onClick = {
                        state.update {
                            it.copy(
                                favorites = if (isFav) it.favorites - app.packageName
                                else it.favorites + app.packageName
                            )
                        }
                    }
                )
                GlassButton(
                    text = if (isHidden) "Показать" else "Скрыть",
                    onClick = {
                        val order = apps.map { a -> a.packageName }
                        state.update {
                            it.copy(
                                order = order,
                                hidden = if (isHidden) it.hidden - app.packageName
                                else it.hidden + app.packageName
                            )
                        }
                    }
                )
                GlassButton(
                    text = "▲",
                    enabled = index > 0,
                    onClick = { moveApp(state, apps, index, index - 1) }
                )
                GlassButton(
                    text = "▼",
                    enabled = index < apps.size - 1,
                    onClick = { moveApp(state, apps, index, index + 1) }
                )
            }
        }
    }
}

private fun moveApp(state: LauncherState, apps: List<AppData>, from: Int, to: Int) {
    val list = apps.map { it.packageName }.toMutableList()
    if (to !in list.indices) return
    val item = list.removeAt(from)
    list.add(to, item)
    state.update { it.copy(order = list) }
}

@Composable
private fun AppearanceTab(state: LauncherState, onNextWallpaper: () -> Unit) {
    val s = state.settings
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        GlassButton(text = "Сменить обои сейчас", onClick = onNextWallpaper)
        GlassButton(
            text = "Смена обоев: ${wallLabel(s.wallSec)}",
            onClick = { state.update { it.copy(wallSec = nextStep(WALL_STEPS, it.wallSec)) } }
        )
        GlassButton(
            text = "Уменьшить движение: ${onOff(s.reducedMotion)}",
            onClick = { state.update { it.copy(reducedMotion = !it.reducedMotion) } }
        )
    }
}

@Composable
private fun ScreensaverTab(state: LauncherState, onPreview: () -> Unit) {
    val s = state.settings
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        GlassButton(
            text = "Заставка: ${onOff(s.ssEnabled)}",
            onClick = { state.update { it.copy(ssEnabled = !it.ssEnabled) } }
        )
        GlassButton(
            text = "Включать через: ${s.ssMinutes} мин",
            onClick = { state.update { it.copy(ssMinutes = nextStep(SS_STEPS, it.ssMinutes)) } }
        )
        GlassButton(
            text = "Показывать часы: ${onOff(s.ssClock)}",
            onClick = { state.update { it.copy(ssClock = !it.ssClock) } }
        )
        GlassButton(text = "Посмотреть заставку", onClick = onPreview)
        Text(
            text = "Заставка включается, если ничего не нажимали. Любая кнопка возвращает на главный экран.",
            fontSize = 22.sp,
            color = Tokens.TextTertiary
        )
    }
}

@Composable
private fun SearchTab(state: LauncherState) {
    val s = state.settings
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = if (s.history.isEmpty()) "История запросов пуста" else "Последние запросы",
            fontSize = 28.sp,
            color = Color.White
        )
        s.history.forEach { q ->
            Text(text = q, fontSize = 26.sp, color = Tokens.TextSecondary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        GlassButton(
            text = "Очистить историю",
            enabled = s.history.isNotEmpty(),
            onClick = { state.update { it.copy(history = emptyList()) } }
        )
    }
}

@Composable
private fun SystemTab(
    versionLabel: String,
    onOpenAndroidSettings: () -> Unit,
    onResetAll: () -> Unit
) {
    var armed by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "Версия лаунчера: ${versionLabel.ifEmpty { "неизвестно" }}",
            fontSize = 28.sp,
            color = Color.White
        )
        Text(
            text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            fontSize = 28.sp,
            color = Color.White
        )
        Text(
            text = "Фото обоев: Wikimedia Commons, лицензия CC BY-SA. Авторы указаны в репозитории проекта.",
            fontSize = 22.sp,
            color = Tokens.TextTertiary
        )
        GlassButton(text = "Открыть настройки Android", onClick = onOpenAndroidSettings)
        GlassButton(
            text = if (armed) "Нажмите ещё раз, чтобы сбросить" else "Сбросить настройки лаунчера",
            onClick = {
                if (armed) onResetAll() else armed = true
            }
        )
        Text(
            text = "Сбрасываются только настройки этого лаунчера: порядок, скрытые, избранное, история, обои и заставка.",
            fontSize = 22.sp,
            color = Tokens.TextTertiary
        )
    }
}

private fun nextStep(steps: List<Int>, current: Int): Int {
    val i = steps.indexOf(current)
    return steps[(if (i < 0) 0 else i + 1) % steps.size]
}

private fun onOff(value: Boolean): String = if (value) "Вкл" else "Выкл"

private fun wallLabel(sec: Int): String = if (sec < 60) "$sec сек" else "${sec / 60} мин"
