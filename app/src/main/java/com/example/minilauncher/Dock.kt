package com.example.minilauncher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Нижняя карусель с приложениями (порядок задаёт пользователь).
 * Фокус возвращается на последнюю плитку: при возврате из приложения и после закрытия настроек.
 */
@Composable
fun AppDock(
    apps: List<AppData>,
    favorites: Set<String>,
    reducedMotion: Boolean,
    focusedPkg: String?,
    restoreTick: Int,
    onFocusedPkg: (String) -> Unit,
    onAccentChange: (Color) -> Unit,
    onLaunch: (AppData) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    // Один FocusRequester на каждое приложение, чтобы можно было вернуть фокус
    val requesters = remember { HashMap<String, FocusRequester>() }

    LaunchedEffect(apps.size, restoreTick) {
        if (apps.isEmpty()) return@LaunchedEffect
        val target = apps.firstOrNull { it.packageName == focusedPkg } ?: apps.first()
        val idx = apps.indexOfFirst { it.packageName == target.packageName }
        listState.scrollToItem(idx)
        // Плитка появляется в композиции не сразу, поэтому пробуем несколько кадров
        repeat(5) {
            val requester = requesters[target.packageName]
            if (requester != null && runCatching { requester.requestFocus() }.isSuccess) {
                onAccentChange(Color(target.accent))
                return@LaunchedEffect
            }
            withFrameNanos { }
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (apps.isEmpty()) {
            Text(
                text = "Приложений пока нет. Откройте «Настройки», чтобы проверить список.",
                fontSize = 26.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        } else {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = Tokens.SafeHorizontal),
                horizontalArrangement = Arrangement.spacedBy(Tokens.RowGap),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val requester = requesters.getOrPut(app.packageName) { FocusRequester() }
                    AppTile(
                        app = app,
                        isFavorite = app.packageName in favorites,
                        reducedMotion = reducedMotion,
                        focusRequester = requester,
                        onFocused = {
                            onFocusedPkg(app.packageName)
                            onAccentChange(Color(app.accent))
                        },
                        onClick = { onLaunch(app) }
                    )
                }
            }
        }
    }
}
