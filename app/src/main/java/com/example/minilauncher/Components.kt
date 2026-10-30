package com.example.minilauncher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val KEY_DP = 80
private const val KEY_GAP_DP = 8

/** Кнопка «стекло»: при фокусе становится белой и чуть увеличивается. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }

    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )
    val bg = when {
        focused -> Color.White
        selected -> Tokens.GlassSelected
        else -> Tokens.Glass
    }
    val fg = when {
        focused -> Color.Black
        !enabled -> Color(0x4DFFFFFF)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .height(Tokens.ButtonHeight)
            .defaultMinSize(minWidth = Tokens.ButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(36.dp))
            .background(bg)
            .bringIntoViewRequester(bringIntoView)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { s ->
                focused = s.isFocused
                if (s.isFocused) scope.launch { bringIntoView.bringIntoView() }
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 26.sp, color = fg, maxLines = 1)
    }
}

/** Картинка приложения: баннер обрезается, иконка вписывается. Если картинки нет, показываем звёздочку. */
@Composable
fun AppThumb(app: AppData, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
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

/** Клавиша экранной клавиатуры. units — ширина в клавишах. */
@Composable
fun KeyCap(
    label: String,
    units: Int,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "keyScale"
    )

    Box(
        modifier = Modifier
            .width((KEY_DP * units + KEY_GAP_DP * (units - 1)).dp)
            .height(KEY_DP.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(if (focused) Color.White else Tokens.Glass)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 30.sp,
            color = if (focused) Color.Black else Color.White,
            maxLines = 1
        )
    }
}
