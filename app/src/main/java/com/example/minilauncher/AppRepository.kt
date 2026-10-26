package com.example.minilauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** Сканирует установленные приложения для ТВ. */
object AppRepository {

    /**
     * false: показываем только пользовательские приложения (как просили).
     * Предустановленные приложения ТВ (YouTube, Netflix и т.п.) в этом случае скрыты.
     * Поставьте true, если хотите видеть и их.
     */
    private const val SHOW_SYSTEM_APPS = false

    fun loadUserApps(context: Context): List<AppData> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

        return pm.queryIntentActivities(query, 0)
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .filter { info ->
                val flags = info.applicationInfo.flags
                SHOW_SYSTEM_APPS ||
                    (flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            }
            .distinctBy { it.packageName }
            .mapNotNull { info ->
                val launch = pm.getLeanbackLaunchIntentForPackage(info.packageName)
                    ?: return@mapNotNull null
                AppData(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.packageName,
                    // Если иконки нет, в UI покажем звёздочку
                    icon = runCatching {
                        info.loadIcon(pm).toBitmap(256, 256).asImageBitmap()
                    }.getOrNull(),
                    launchIntent = launch
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
