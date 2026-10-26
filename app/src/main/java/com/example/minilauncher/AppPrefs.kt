package com.example.minilauncher

import android.content.Context

/** Порядок приложений и список скрытых. Сохраняются между запусками. */
object AppPrefs {
    private const val FILE = "launcher"
    private const val KEY_ORDER = "order"
    private const val KEY_HIDDEN = "hidden"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun order(ctx: Context): List<String> =
        prefs(ctx).getString(KEY_ORDER, "").orEmpty().split(',').filter { it.isNotEmpty() }

    fun hidden(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()

    fun save(ctx: Context, order: List<String>, hidden: Set<String>) {
        prefs(ctx).edit()
            .putString(KEY_ORDER, order.joinToString(","))
            .putStringSet(KEY_HIDDEN, hidden.toSet())
            .apply()
    }

    /** Приложения в сохранённом порядке. Новые приложения добавляются в конец, по алфавиту. */
    fun arrange(all: List<AppData>, order: List<String>): List<AppData> {
        val byPkg = all.associateBy { it.packageName }
        val ordered = order.mapNotNull { byPkg[it] }
        val known = order.toSet()
        return ordered + all.filter { it.packageName !in known }
    }
}
