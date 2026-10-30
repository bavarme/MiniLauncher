package com.example.minilauncher

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private const val MAX_HISTORY = 8

/**
 * Главный экран. Поверх карусели по очереди открываются поиск, настройки и меню приложения.
 * «Назад» закрывает открытый оверлей; на главном экране он ничего не делает.
 */
@Composable
fun LauncherScreen(state: LauncherState, seeds: List<String>, versionLabel: String) {
    val context = LocalContext.current
    val s = state.settings
    val reduced = s.reducedMotion

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    BackHandler(enabled = state.overlay != Overlay.NONE) { state.close() }

    // Список приложений перечитывается при установке, удалении и обновлении (appsVersion)
    val allApps by produceState(initialValue = emptyList<AppData>(), state.appsVersion) {
        value = withContext(Dispatchers.IO) { AppRepository.loadUserApps(context) }
    }
    val arranged = remember(allApps, s.order, seeds) { arrangeApps(allApps, s.order, seeds) }
    val visible = remember(arranged, s.hidden) { arranged.filter { it.packageName !in s.hidden } }
    val menuApp = arranged.firstOrNull { it.packageName == state.menuPkg }
    val menuIndex = visible.indexOfFirst { it.packageName == state.menuPkg }

    // Если приложение меню удалили, меню закрываем
    LaunchedEffect(state.overlay, menuApp) {
        if (state.overlay == Overlay.MENU && menuApp == null) state.close()
    }

    // Смена обоев по таймеру. wallTick перезапускает таймер после ручной смены.
    var wallIndex by remember { mutableIntStateOf(0) }
    var wallTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(s.wallSec, wallTick) {
        while (true) {
            delay(s.wallSec * 1000L)
            wallIndex = (wallIndex + 1) % WALLPAPERS.size
        }
    }

    // Подсветка снизу меняется под цвет приложения в фокусе
    var accentTarget by remember { mutableStateOf(Color(0xFF1C1C1E)) }
    val accent by animateColorAsState(
        targetValue = accentTarget,
        animationSpec = tween(durationMillis = if (reduced) 0 else 700),
        label = "accent"
    )

    fun openApp(app: AppData) {
        runCatching { context.startActivity(app.launchIntent) }
            .onFailure { toast("Не удалось открыть «${app.label}»") }
    }

    fun openDetails(pkg: String) {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", pkg, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { toast("Не удалось открыть сведения о приложении") }
    }

    fun rememberQuery(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        state.update { st ->
            st.copy(
                history = (listOf(clean) + st.history.filter { !it.equals(clean, ignoreCase = true) })
                    .take(MAX_HISTORY)
            )
        }
    }

    fun toggleFavorite(pkg: String) {
        state.update { st ->
            st.copy(favorites = if (pkg in st.favorites) st.favorites - pkg else st.favorites + pkg)
        }
    }

    // Переставляет видимое приложение на соседнее место среди видимых.
    // Скрытые приложения остаются на своих местах.
    fun moveVisible(pkg: String, delta: Int) {
        val vis = visible.map { it.packageName }.toMutableList()
        val i = vis.indexOf(pkg)
        val j = i + delta
        if (i < 0 || j !in vis.indices) return
        java.util.Collections.swap(vis, i, j)

        val all = arranged.map { it.packageName }.toMutableList()
        val slots = all.indices.filter { all[it] !in s.hidden }
        slots.forEachIndexed { k, slot -> all[slot] = vis[k] }
        state.update { st -> st.copy(order = all) }
    }

    fun hide(pkg: String) {
        val vis = visible.map { it.packageName }
        val i = vis.indexOf(pkg)
        // Фокус переходит на соседнее приложение, чтобы не прыгать на первое
        state.focusedPkg = vis.getOrNull(i + 1) ?: vis.getOrNull(i - 1)
        val order = arranged.map { it.packageName }
        state.update { st -> st.copy(order = order, hidden = st.hidden + pkg) }
        state.close()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HomeBackground(wallIndex = wallIndex, accent = accent, reducedMotion = reduced)

        when (state.overlay) {
            Overlay.NONE -> Column(modifier = Modifier.fillMaxSize()) {
                HomeTop(
                    onSearch = { state.open(Overlay.SEARCH) },
                    onSettings = { state.open(Overlay.SETTINGS) },
                    modifier = Modifier.weight(0.6f)
                )
                AppDock(
                    apps = visible,
                    favorites = s.favorites,
                    reducedMotion = reduced,
                    focusedPkg = state.focusedPkg,
                    restoreTick = state.restoreTick,
                    onFocusedPkg = { state.focusedPkg = it },
                    onAccentChange = { accentTarget = it },
                    onLaunch = { openApp(it) },
                    modifier = Modifier.weight(0.4f)
                )
            }

            Overlay.SEARCH -> SearchScreen(
                apps = arranged,
                history = s.history,
                onLaunch = { app, query ->
                    rememberQuery(query)
                    state.close()
                    openApp(app)
                },
                onClearHistory = { state.update { st -> st.copy(history = emptyList()) } },
                onClose = { state.close() }
            )

            Overlay.SETTINGS -> SettingsScreen(
                state = state,
                apps = arranged,
                versionLabel = versionLabel,
                onNextWallpaper = {
                    wallIndex = (wallIndex + 1) % WALLPAPERS.size
                    wallTick++
                },
                onPreviewScreensaver = {
                    state.close()
                    state.screensaverOn = true
                },
                onOpenAndroidSettings = {
                    runCatching {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onFailure { toast("Не удалось открыть настройки Android") }
                },
                onResetAll = {
                    state.resetAll()
                    state.close()
                },
                onClose = { state.close() }
            )

            Overlay.MENU -> if (menuApp != null) {
                AppContextMenu(
                    app = menuApp,
                    isFavorite = menuApp.packageName in s.favorites,
                    canMoveLeft = menuIndex > 0,
                    canMoveRight = menuIndex in 0 until visible.lastIndex,
                    onOpen = {
                        state.close()
                        openApp(menuApp)
                    },
                    onToggleFavorite = { toggleFavorite(menuApp.packageName) },
                    onMoveLeft = { moveVisible(menuApp.packageName, -1) },
                    onMoveRight = { moveVisible(menuApp.packageName, 1) },
                    onHide = { hide(menuApp.packageName) },
                    onInfo = {
                        state.close()
                        openDetails(menuApp.packageName)
                    },
                    onClose = { state.close() }
                )
            }
        }

        if (state.overlay == Overlay.NONE && state.screensaverOn) {
            ScreenSaver(
                wallSec = s.wallSec,
                showClock = s.ssClock,
                reducedMotion = reduced
            )
        }
    }
}
