package com.example.minilauncher

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Нижняя карусель с приложениями. Выбранная плитка подсвечивает фон цветом приложения.
 * onAccentChange сообщает наверх, какой цвет подсвечивать.
 */
@Composable
fun AppDock(
    apps: List<AppData>,
    onAccentChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firstFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(-1) }

    // Фокус на первую плитку, чтобы пульт сразу работал
    LaunchedEffect(apps.size) {
        if (apps.isNotEmpty()) runCatching { firstFocus.requestFocus() }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (apps.isEmpty()) {
            Text(
                text = "Приложений пока нет. Нажмите «Настроить», чтобы управлять списком.",
                fontSize = 26.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    AppTile(
                        app = app,
                        index = index,
                        focusedIndex = focusedIndex,
                        focusRequester = if (index == 0) firstFocus else null,
                        onFocused = {
                            focusedIndex = index
                            onAccentChange(Color(app.accent))
                        },
                        onClick = { openApp(context, app) }
                    )
                }
            }
        }
    }
}

private fun openApp(context: Context, app: AppData) {
    try {
        context.startActivity(app.launchIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Не открылось: ${app.label}", Toast.LENGTH_LONG).show()
    }
}
