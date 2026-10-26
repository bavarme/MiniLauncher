package com.example.minilauncher

import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap

/** Приложение для карусели: название, иконка и intent для запуска. */
data class AppData(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap?,
    val launchIntent: Intent
)
