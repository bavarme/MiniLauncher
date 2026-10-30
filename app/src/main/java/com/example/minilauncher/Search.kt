package com.example.minilauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RU_ROW_1 = "йцукенгшщзх".map { it.toString() }
private val RU_ROW_2 = "фывапролджэё".map { it.toString() }
private val RU_ROW_3 = "ячсмитьбъ".map { it.toString() }
private val EN_ROW_1 = "qwertyuiop".map { it.toString() }
private val EN_ROW_2 = "asdfghjkl".map { it.toString() }
private val EN_ROW_3 = "zxcvbnm".map { it.toString() }
private val DIGIT_ROW = "1234567890".map { it.toString() }

/**
 * Поиск и библиотека: экранная клавиатура (RU/EN), список приложений по алфавиту,
 * фильтр по запросу и история запросов. Работает без интернета.
 */
@Composable
fun SearchScreen(
    apps: List<AppData>,
    history: List<String>,
    onLaunch: (AppData, String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var ru by rememberSaveable { mutableStateOf(true) }
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }

    val results = remember(query, apps) {
        val q = query.trim()
        val sorted = apps.sortedBy { it.label.lowercase() }
        if (q.isEmpty()) sorted else sorted.filter { it.label.contains(q, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.Scrim)
            .padding(horizontal = Tokens.SafeHorizontal, vertical = 64.dp)
    ) {
        Text(
            text = if (query.isEmpty()) "Поиск" else query,
            fontSize = 56.sp,
            color = if (query.isEmpty()) Tokens.TextTertiary else Tokens.TextPrimary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (history.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(history) { item ->
                        GlassButton(text = item, onClick = { query = item })
                    }
                }
                GlassButton(text = "Очистить историю", onClick = onClearHistory)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val letters1 = if (ru) RU_ROW_1 else EN_ROW_1
                val letters2 = if (ru) RU_ROW_2 else EN_ROW_2
                val letters3 = if (ru) RU_ROW_3 else EN_ROW_3
                KeyRow(keys = letters1, firstKey = firstKey) { query += it }
                KeyRow(keys = letters2, firstKey = null) { query += it }
                KeyRow(keys = letters3, firstKey = null) { query += it }
                KeyRow(keys = DIGIT_ROW, firstKey = null) { query += it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyCap(label = if (ru) "RU" else "EN", units = 2, onClick = { ru = !ru })
                    KeyCap(label = "Пробел", units = 3, onClick = { query += " " })
                    KeyCap(label = "←", units = 2, onClick = {
                        if (query.isNotEmpty()) query = query.dropLast(1)
                    })
                    KeyCap(label = "Очистить", units = 3, onClick = { query = "" })
                }
            }

            Column(modifier = Modifier.weight(0.42f)) {
                if (results.isEmpty()) {
                    Text(
                        text = "Ничего не найдено",
                        fontSize = 28.sp,
                        color = Tokens.TextSecondary
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(results, key = { it.packageName }) { app ->
                            ResultRow(app = app) { onLaunch(app, query) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRow(keys: List<String>, firstKey: FocusRequester?, onKey: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEachIndexed { i, key ->
            KeyCap(
                label = key,
                units = 1,
                focusRequester = if (i == 0) firstKey else null,
                onClick = { onKey(key) }
            )
        }
    }
}

@Composable
private fun ResultRow(app: AppData, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (focused) Color.White else Tokens.GlassRow)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AppThumb(app = app, width = 112.dp, height = 64.dp)
        Text(
            text = app.label,
            fontSize = 26.sp,
            color = if (focused) Color.Black else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
