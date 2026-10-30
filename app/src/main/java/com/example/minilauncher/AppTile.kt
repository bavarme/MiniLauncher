package com.example.minilauncher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val TILE = Tokens.TileSize

/**
 * Плитка приложения. В фокусе плитка чуть увеличивается, получает белую рамку и мягкое
 * свечение цветом приложения. Всё это делается при отрисовке, без пересчёта композиции,
 * поэтому листание не тормозит. Баннер 16:9 показывается целиком на размытом фоне,
 * иконка заполняет плитку.
 */
@Composable
fun AppTile(
    app: AppData,
    isFavorite: Boolean,
    reducedMotion: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }
    val accent = Color(app.accent)
    val shape = RoundedCornerShape(Tokens.TileRadius)

    val scale by animateFloatAsState(
        targetValue = if (focused && !reducedMotion) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tileScale"
    )
    val focusAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = Tokens.MS_FOCUS),
        label = "focusAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.bringIntoViewRequester(bringIntoView)
    ) {
        Box(
            modifier = Modifier
                .size(TILE)
                // Свечение рисуется без обрезки и без blur: радиальный градиент дешёвый
                .drawBehind {
                    val a = focusAlpha
                    if (a > 0f) {
                        val r = size.minDimension * 0.62f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.35f * a), Color.Transparent),
                                center = center,
                                radius = r
                            ),
                            radius = r,
                            center = center
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF2C2C2E),
                        1f to Color(0xFF141416)
                    )
                )
                .then(if (focused) Modifier.border(3.dp, Color.White, shape) else Modifier)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { s ->
                    focused = s.isFocused
                    if (s.isFocused) {
                        onFocused()
                        scope.launch { bringIntoView.bringIntoView() }
                    }
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val art = app.art
            val backdrop = app.backdrop
            when {
                art != null && app.isBanner && backdrop != null -> {
                    Image(
                        bitmap = backdrop,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)))
                    Image(
                        bitmap = art,
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                art != null -> Image(
                    bitmap = art,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Text(text = "★", fontSize = 96.sp, color = Color.White)
            }

            // Лёгкий блик сверху, как у стеклянной поверхности
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x1FFFFFFF),
                            0.5f to Color.Transparent
                        )
                    )
            )

            if (isFavorite) {
                Text(
                    text = "★",
                    fontSize = 28.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                )
            }
        }

        // Название под плиткой: видно только в фокусе
        Box(
            modifier = Modifier
                .width(TILE + 60.dp)
                .height(80.dp)
                .padding(top = 22.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = app.label,
                fontSize = 30.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = focusAlpha }
            )
        }
    }
}
