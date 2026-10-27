package com.example.minilauncher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Экран настроек: список приложений, скрыть или показать, поменять порядок кнопками ▲ ▼.
 * Изменения сохраняются сразу.
 */
@Composable
fun SettingsScreen(
    apps: List<AppData>,
    hidden: Set<String>,
    onMove: (from: Int, to: Int) -> Unit,
    onToggle: (packageName: String) -> Unit,
    onClose: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .padding(horizontal = 160.dp, vertical = 90.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Настройки",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Скрывайте приложения и меняйте их порядок. Изменения сохраняются.",
                    fontSize = 22.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            GlassButton(text = "Закрыть", onClick = onClose)
        }

        Spacer(modifier = Modifier.height(40.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                SettingsRow(
                    app = app,
                    index = index,
                    count = apps.size,
                    isHidden = app.packageName in hidden,
                    focusRequester = if (index == 0) firstFocus else null,
                    onMove = onMove,
                    onToggle = { onToggle(app.packageName) }
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    app: AppData,
    index: Int,
    count: Int,
    isHidden: Boolean,
    focusRequester: FocusRequester?,
    onMove: (Int, Int) -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0x14FFFFFF))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Thumb(app)
        Text(
            text = app.label,
            fontSize = 30.sp,
            color = if (isHidden) Color.White.copy(alpha = 0.4f) else Color.White,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        GlassButton(
            text = if (isHidden) "Показать" else "Скрыть",
            onClick = onToggle,
            focusRequester = focusRequester
        )
        GlassButton(
            text = "▲",
            enabled = index > 0,
            onClick = { onMove(index, index - 1) }
        )
        GlassButton(
            text = "▼",
            enabled = index < count - 1,
            onClick = { onMove(index, index + 1) }
        )
    }
}

/** Маленькая картинка приложения в строке настроек. */
@Composable
private fun Thumb(app: AppData) {
    Box(
        modifier = Modifier
            .size(width = 128.dp, height = 72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(app.accent)),
        contentAlignment = Alignment.Center
    ) {
        val art = app.art
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = if (app.isBanner) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = "★", fontSize = 28.sp, color = Color.White)
        }
    }
}
