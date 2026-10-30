package com.example.minilauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat

/**
 * Единственная Activity лаунчера. Отвечает за кнопки пульта, заставку по простою
 * и обновление списка при установке, удалении или обновлении приложений.
 */
class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var state: LauncherState

    private val handler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable { startScreensaverIfIdle() }

    // Клавиша, отпускание которой надо проглотить: после пробуждения заставки
    // иначе отпускание нажмёт плитку под фокусом
    private var swallowKeyUp: Int? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            state.appsVersion++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        state = LauncherState(prefs, prefs.load(), prefs.lastFocus)

        // Главный экран не закрывается «Назад». Этот обработчик стоит первым в очереди,
        // поэтому BackHandler открытых оверлеев срабатывает раньше.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // намеренно пусто
            }
        })

        val seeds = resources.getStringArray(R.array.dock_apps).toList()
        setContent {
            DesignScale {
                LauncherScreen(state = state, seeds = seeds, versionLabel = versionName())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Возврат из другого приложения: фокус на последнюю плитку
        state.restoreTick++
        userActive()
    }

    override fun onPause() {
        prefs.lastFocus = state.focusedPkg
        handler.removeCallbacks(idleRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Нажали «Домой» на открытом лаунчере: закрываем оверлеи и будим заставку
        if (state.overlay != Overlay.NONE) state.close()
        state.screensaverOn = false
        userActive()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && event.keyCode == swallowKeyUp) {
            swallowKeyUp = null
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            userActive()
            if (state.screensaverOn) {
                // Любая кнопка только будит заставку и ничего не нажимает на экране
                state.screensaverOn = false
                swallowKeyUp = event.keyCode
                return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_MENU && event.repeatCount == 0) {
                toggleMenu()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Кнопка «Меню»: открывает меню приложения в фокусе (или настройки, если приложение
     * ещё не выбрано). Если что-то уже открыто, закрывает.
     */
    private fun toggleMenu() {
        if (state.overlay != Overlay.NONE) {
            state.close()
            return
        }
        val pkg = state.focusedPkg
        if (pkg != null) {
            state.menuPkg = pkg
            state.open(Overlay.MENU)
        } else {
            state.open(Overlay.SETTINGS)
        }
    }

    /** Любое нажатие сбрасывает отсчёт простоя. Время берётся из настроек в этот момент. */
    private fun userActive() {
        handler.removeCallbacks(idleRunnable)
        val s = state.settings
        if (s.ssEnabled) handler.postDelayed(idleRunnable, s.ssMinutes * 60_000L)
    }

    private fun startScreensaverIfIdle() {
        if (state.settings.ssEnabled && state.overlay == Overlay.NONE) {
            state.screensaverOn = true
        }
    }

    private fun versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    }.getOrDefault("")
}
