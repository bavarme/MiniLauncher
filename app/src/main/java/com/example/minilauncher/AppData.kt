package com.example.minilauncher

import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Приложение для карусели.
 * art — баннер для ТВ (широкая картинка) или иконка, если баннера нет.
 * isBanner — true, если art это баннер (его обрезаем, а иконку вписываем).
 * accent — основной цвет картинки ARGB, из него делаем подсветку и фон плитки.
 */
data class AppData(
    val label: String,
    val packageName: String,
    val art: ImageBitmap?,
    val isBanner: Boolean,
    val accent: Int,
    val launchIntent: Intent,
    /** Размытый фон для баннера (картинка 16×9, растянутая обратно). Для иконок null. */
    val backdrop: ImageBitmap? = null
)
