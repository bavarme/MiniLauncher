package com.example.minilauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Меню приложения. Открывается кнопкой «Меню» на пульте, когда фокус на плитке.
 * Обычный выбор (OK) по-прежнему сразу запускает приложение.
 */
@Composable
fun AppContextMenu(
    app: AppData,
    isFavorite: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onClose: () -> Unit
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AppThumb(app = app, width = 160.dp, height = 90.dp)
                Text(
                    text = app.label,
                    fontSize = 36.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            GlassButton(
                text = "Открыть",
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                focusRequester = first
            )
            GlassButton(
                text = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                onClick = onToggleFavorite,
                modifier = Modifier.fillMaxWidth()
            )
            if (canMoveLeft) {
                GlassButton(
                    text = "Переместить влево",
                    onClick = onMoveLeft,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (canMoveRight) {
                GlassButton(
                    text = "Переместить вправо",
                    onClick = onMoveRight,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            GlassButton(
                text = "Скрыть с главного экрана",
                onClick = onHide,
                modifier = Modifier.fillMaxWidth()
            )
            GlassButton(
                text = "Сведения о приложении",
                onClick = onInfo,
                modifier = Modifier.fillMaxWidth()
            )
            GlassButton(
                text = "Закрыть",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
