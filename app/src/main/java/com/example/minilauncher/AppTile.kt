package com.example.minilauncher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val TILE = 240.dp
private val TILE_RADIUS = 56.dp

/**
 * Плитка приложения в стиле tvOS: скруглённый квадрат с цветом приложения.
 * В фокусе плитка увеличивается с пружинкой, получает тень в цвет приложения,
 * белую рамку и название под собой. Соседние плитки отъезжают в стороны.
 */
@Composable
fun AppTile(
    app: AppData,
    index: Int,
    focusedIndex: Int,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }
    val accent = Color(app.accent)
    val shape = RoundedCornerShape(TILE_RADIUS)

    val scale by animateFloatAsState(
        targetValue = if (focused) 1.14f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tileScale"
    )
    val magnet by animateDpAsState(
        targetValue = when {
            focusedIndex < 0 || index == focusedIndex -> 0.dp
            index == focusedIndex - 1 -> (-28).dp
            index == focusedIndex + 1 -> 28.dp
            else -> 0.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "magnet"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "labelAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(x = magnet)
            .bringIntoViewRequester(bringIntoView)
    ) {
        Box(
            modifier = Modifier
                .size(TILE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = if (focused) 40.dp else 0.dp,
                    shape = shape,
                    ambientColor = accent,
                    spotColor = accent
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        0f to lerp(accent, Color.White, 0.25f),
                        1f to lerp(accent, Color.Black, 0.45f)
                    )
                )
                .then(if (focused) Modifier.border(4.dp, Color.White, shape) else Modifier)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { state ->
                    focused = state.isFocused
                    if (state.isFocused) {
                        onFocused()
                        scope.launch { bringIntoView.bringIntoView() }
                    }
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val art = app.art
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = app.label,
                    contentScale = if (app.isBanner) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (app.isBanner) 0.dp else 40.dp)
                )
            } else {
                // Иконки нет: показываем звёздочку
                Text(text = "★", fontSize = 96.sp, color = Color.White)
            }
            // Блик сверху, как у стеклянной поверхности
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x2EFFFFFF),
                            0.5f to Color.Transparent
                        )
                    )
            )
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
                modifier = Modifier.graphicsLayer { alpha = labelAlpha }
            )
        }
    }
}
