package com.example.minilauncher

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeView: TextView
    private lateinit var dateView: TextView
    private lateinit var dock: LinearLayout

    // Обновляем часы каждую секунду, пока активность видна
    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    // Для каждого приложения перечисляем возможные пакеты (первый найденный пойдёт в док)
    private val dockCandidates = listOf(
        listOf("com.google.android.youtube.tv", "com.google.android.youtube"),
        listOf("com.netflix.ninja", "com.netflix.mediaclient"),
        listOf("com.android.chrome", "com.google.android.apps.chrome"),
        listOf("com.android.tv.settings", "com.android.settings")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeView = findViewById(R.id.timeView)
        dateView = findViewById(R.id.dateView)
        dock = findViewById(R.id.dock)

        buildDock()
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockTick)
        // Фокус на первую иконку дока, чтобы пульт сразу работал
        dock.post { dock.getChildAt(0)?.requestFocus() }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockTick)
    }

    // Лаунчер — домашний экран, кнопку "Назад" не перехватываем
    override fun onBackPressed() {
        // ничего не делаем
    }

    private fun updateClock() {
        val now = Date()
        timeView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        dateView.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
            .format(now)
            .replaceFirstChar { it.uppercase() }
    }

    private fun buildDock() {
        val pm = packageManager

        for (candidates in dockCandidates) {
            for (pkg in candidates) {
                val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
                    ?: pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    val icon = pm.getApplicationIcon(pkg)
                    addDockItem(icon, intent)
                    break   // нашли — переходим к следующему приложению
                }
            }
        }
    }

    private fun addDockItem(icon: Drawable, intent: Intent) {
        val size = dp(130)

        val item = ImageView(this).apply {
            setImageDrawable(icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            isFocusable = true
            isFocusableInTouchMode = false
            isClickable = true
            background = cardBackground(false)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(dp(24), 0, dp(24), 0)
            }

            // Фокус: плавно увеличиваем и подсвечиваем
            setOnFocusChangeListener { v, hasFocus ->
                v.background = cardBackground(hasFocus)
                v.animate()
                    .scaleX(if (hasFocus) 1.3f else 1f)
                    .scaleY(if (hasFocus) 1.3f else 1f)
                    .setDuration(180)
                    .start()
            }

            setOnClickListener { startActivity(intent) }
        }

        dock.addView(item)
    }

    private fun cardBackground(focused: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(28).toFloat()
        setColor(if (focused) Color.parseColor("#2C2C2E") else Color.parseColor("#1C1C1E"))
        setStroke(
            dp(2),
            if (focused) Color.WHITE else Color.TRANSPARENT
        )
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
