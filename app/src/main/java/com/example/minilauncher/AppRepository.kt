package com.example.minilauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Сканирует установленные приложения для ТВ.
 * Картинки уменьшаются сразу при загрузке, чтобы не занимать лишнюю память.
 */
object AppRepository {

    /** false: только пользовательские приложения. true: показывать и предустановленные. */
    private const val SHOW_SYSTEM_APPS = false

    private val FALLBACK_ACCENT: Int = 0xFF3A3A3C.toInt()

    fun loadUserApps(context: Context): List<AppData> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

        return pm.queryIntentActivities(query, 0)
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .filter { isVisibleApp(it) }
            .distinctBy { it.packageName }
            .mapNotNull { toAppData(pm, it) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun isVisibleApp(info: ActivityInfo): Boolean {
        if (SHOW_SYSTEM_APPS) return true
        val flags = info.applicationInfo.flags
        return (flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
            (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun toAppData(pm: PackageManager, info: ActivityInfo): AppData? {
        val launch = pm.getLeanbackLaunchIntentForPackage(info.packageName) ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        // Баннер 16:9 лучше иконки. ARGB_8888, потому что RGB_565 даёт полосы на градиентах
        val banner = runCatching {
            (info.loadBanner(pm) ?: info.applicationInfo.loadBanner(pm))
                ?.toBitmap(640, 360, Bitmap.Config.ARGB_8888)
        }.getOrNull()
        val icon = if (banner == null) {
            runCatching { info.loadIcon(pm).toBitmap(512, 512, Bitmap.Config.ARGB_8888) }.getOrNull()
        } else {
            null
        }
        val source = banner ?: icon

        // Размытый фон для баннера: картинка 16×9 растянута обратно, поэтому размытие почти бесплатное
        val backdrop = banner?.let { Bitmap.createScaledBitmap(it, 16, 9, true).asImageBitmap() }

        return AppData(
            label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(info.packageName),
            packageName = info.packageName,
            art = source?.asImageBitmap(),
            isBanner = banner != null,
            accent = source?.let { averageColor(it) } ?: FALLBACK_ACCENT,
            launchIntent = launch,
            backdrop = backdrop
        )
    }

    /** Средний цвет картинки по сетке 8×8, прозрачные пиксели пропускаем. */
    private fun averageColor(src: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(src, 8, 8, true)
        var r = 0
        var g = 0
        var b = 0
        var n = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val p = small.getPixel(x, y)
                if (Color.alpha(p) < 128) continue
                r += Color.red(p)
                g += Color.green(p)
                b += Color.blue(p)
                n++
            }
        }
        small.recycle()
        return if (n == 0) FALLBACK_ACCENT else Color.rgb(r / n, g / n, b / n)
    }
}

/**
 * Порядок приложений: сохранённый, а если его ещё нет, стартовый набор из config.xml.
 * Новые приложения добавляются в конец, по алфавиту. Удалённые просто пропускаются.
 */
fun arrangeApps(all: List<AppData>, saved: List<String>, seeds: List<String>): List<AppData> {
    val byPkg = all.associateBy { it.packageName }
    val base = if (saved.isEmpty()) seeds else saved
    val ordered = base.mapNotNull { byPkg[it] }
    val known = base.toSet()
    return ordered + all.filter { it.packageName !in known }
}
