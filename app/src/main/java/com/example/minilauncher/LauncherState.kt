package com.example.minilauncher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Что открыто поверх главного экрана. */
enum class Overlay { NONE, SETTINGS, SEARCH, MENU }

/** Состояние лаунчера, общее для Activity и всех экранов. */
class LauncherState(
    private val prefs: AppPrefs,
    initial: LauncherSettings,
    initialFocus: String?
) {
    var settings by mutableStateOf(initial)
        private set

    var overlay by mutableStateOf(Overlay.NONE)
        private set

    var menuPkg by mutableStateOf<String?>(null)
    var focusedPkg by mutableStateOf(initialFocus)
    var screensaverOn by mutableStateOf(false)

    /** Меняется при возврате на главный экран: по нему фокус возвращается на последнюю плитку. */
    var restoreTick by mutableIntStateOf(0)

    /** Меняется при установке, удалении или обновлении приложения. */
    var appsVersion by mutableIntStateOf(0)

    fun open(o: Overlay) {
        overlay = o
    }

    fun close() {
        overlay = Overlay.NONE
        restoreTick++
    }

    fun update(block: (LauncherSettings) -> LauncherSettings) {
        settings = block(settings)
        prefs.save(settings)
    }

    fun resetAll() {
        prefs.clear()
        settings = prefs.load()
        focusedPkg = null
        menuPkg = null
    }
}
