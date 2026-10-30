package com.example.minilauncher

import android.content.Context

/** Все настройки лаунчера. Хранятся только на этом устройстве. */
data class LauncherSettings(
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val favorites: Set<String> = emptySet(),
    val history: List<String> = emptyList(),
    val wallSec: Int = 30,
    val reducedMotion: Boolean = false,
    val ssEnabled: Boolean = true,
    val ssMinutes: Int = 5,
    val ssClock: Boolean = true
)

/** Чтение и запись настроек в SharedPreferences. */
class AppPrefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): LauncherSettings = LauncherSettings(
        order = list(KEY_ORDER, ","),
        hidden = sp.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet(),
        favorites = sp.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet(),
        history = list(KEY_HISTORY, HISTORY_SEP),
        wallSec = sp.getInt(KEY_WALL_SEC, 30),
        reducedMotion = sp.getBoolean(KEY_REDUCED, false),
        ssEnabled = sp.getBoolean(KEY_SS_ON, true),
        ssMinutes = sp.getInt(KEY_SS_MIN, 5),
        ssClock = sp.getBoolean(KEY_SS_CLOCK, true)
    )

    fun save(s: LauncherSettings) {
        sp.edit()
            .putString(KEY_ORDER, s.order.joinToString(","))
            .putStringSet(KEY_HIDDEN, HashSet(s.hidden))
            .putStringSet(KEY_FAVORITES, HashSet(s.favorites))
            .putString(KEY_HISTORY, s.history.joinToString(HISTORY_SEP))
            .putInt(KEY_WALL_SEC, s.wallSec)
            .putBoolean(KEY_REDUCED, s.reducedMotion)
            .putBoolean(KEY_SS_ON, s.ssEnabled)
            .putInt(KEY_SS_MIN, s.ssMinutes)
            .putBoolean(KEY_SS_CLOCK, s.ssClock)
            .apply()
    }

    /** Последнее приложение с фокусом: чтобы вернуть фокус после пересоздания Activity. */
    var lastFocus: String?
        get() = sp.getString(KEY_LAST_FOCUS, null)
        set(value) {
            sp.edit().putString(KEY_LAST_FOCUS, value).apply()
        }

    /** Удаляет только данные самого лаунчера, остальные данные ТВ не трогаем. */
    fun clear() {
        sp.edit().clear().apply()
    }

    private fun list(key: String, sep: String): List<String> =
        sp.getString(key, "").orEmpty().split(sep).filter { it.isNotEmpty() }

    private companion object {
        const val FILE = "launcher"
        const val KEY_ORDER = "order"
        const val KEY_HIDDEN = "hidden"
        const val KEY_FAVORITES = "favorites"
        const val KEY_HISTORY = "history"
        const val KEY_WALL_SEC = "wall_sec"
        const val KEY_REDUCED = "reduced_motion"
        const val KEY_SS_ON = "ss_enabled"
        const val KEY_SS_MIN = "ss_minutes"
        const val KEY_SS_CLOCK = "ss_clock"
        const val KEY_LAST_FOCUS = "last_focus"
        const val HISTORY_SEP = "\u001F"
    }
}
