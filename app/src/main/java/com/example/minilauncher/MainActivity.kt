package com.example.minilauncher

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: View
    private lateinit var timeView: TextView
    private lateinit var dateView: TextView
    private lateinit var appsList: RecyclerView
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: AppAdapter

    // Часы обновляем раз в 15 секунд: этого достаточно для HH:mm и не греет ТВ
    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        timeView = findViewById(R.id.timeView)
        dateView = findViewById(R.id.dateView)
        appsList = findViewById(R.id.appsList)
        prefs = getSharedPreferences("launcher", MODE_PRIVATE)

        appsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = AppAdapter(
            onClick = { app -> startActivity(app.intent) },
            onLongClick = { app -> confirmHide(app) }
        )
        appsList.adapter = adapter

        applyWallpaper(prefs.getInt(KEY_WALLPAPER, 0))
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockTick)
        reloadApps()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockTick)
    }

    // Домашний экран: "Назад" ничего не закрывает
    override fun onBackPressed() {
        // ничего не делаем
    }

    // Кнопка "Меню" на пульте открывает настройки лаунчера
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showMenu()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateClock() {
        val now = Date()
        val locale = Locale.getDefault()
        timeView.text = SimpleDateFormat("HH:mm", locale).format(now)
        dateView.text = SimpleDateFormat("EEEE, d MMMM", locale)
            .format(now)
            .replaceFirstChar { it.uppercase() }
    }

    private fun reloadApps() {
        val hidden = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()
        adapter.submit(queryApps().filter { it.packageName !in hidden })
        appsList.post {
            appsList.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
        }
    }

    // Все приложения, которые можно запустить на ТВ, без самого лаунчера
    private fun queryApps(): List<AppInfo> {
        val pm = packageManager
        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(leanback, 0)
            .ifEmpty { pm.queryIntentActivities(launcher, 0) }

        return resolved
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = ri.loadIcon(pm),
                    intent = Intent(Intent.ACTION_MAIN).setClassName(pkg, ri.activityInfo.name)
                )
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    // Долгое нажатие на приложение: предложить скрыть его с главного экрана
    private fun confirmHide(app: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setMessage("Скрыть с главного экрана?")
            .setPositiveButton("Скрыть") { _, _ ->
                val hidden = prefs.getStringSet(KEY_HIDDEN, emptySet())?.toMutableSet()
                    ?: mutableSetOf()
                hidden.add(app.packageName)
                prefs.edit().putStringSet(KEY_HIDDEN, hidden).apply()
                reloadApps()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showMenu() {
        val hiddenCount = prefs.getStringSet(KEY_HIDDEN, emptySet())?.size ?: 0
        val options = arrayOf(
            "Сменить обои",
            "Показать скрытые приложения ($hiddenCount)"
        )
        AlertDialog.Builder(this)
            .setTitle("Mini Launcher")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cycleWallpaper()
                    1 -> {
                        prefs.edit().remove(KEY_HIDDEN).apply()
                        reloadApps()
                    }
                }
            }
            .show()
    }

    private fun cycleWallpaper() {
        val next = (prefs.getInt(KEY_WALLPAPER, 0) + 1) % WALLPAPERS.size
        prefs.edit().putInt(KEY_WALLPAPER, next).apply()
        applyWallpaper(next)
    }

    // Обои — это градиент: ничего не нужно загружать, почти не занимает память
    private fun applyWallpaper(index: Int) {
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            WALLPAPERS[index]
        )
    }

    companion object {
        private const val KEY_WALLPAPER = "wallpaper"
        private const val KEY_HIDDEN = "hidden"

        private val WALLPAPERS = listOf(
            intArrayOf(Color.parseColor("#1E2A4A"), Color.parseColor("#05070F")), // ночь
            intArrayOf(Color.parseColor("#3B1E4A"), Color.parseColor("#0A0510")), // сумерки
            intArrayOf(Color.parseColor("#0F3D3E"), Color.parseColor("#03100F")), // изумруд
            intArrayOf(Color.parseColor("#4A2B1E"), Color.parseColor("#0F0805")), // закат
            intArrayOf(Color.parseColor("#2C2C2E"), Color.parseColor("#000000"))  // графит
        )
    }
}
