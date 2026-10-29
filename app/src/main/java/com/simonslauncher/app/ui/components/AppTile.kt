package com.simonslauncher.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.simonslauncher.app.ui.theme.Amber
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.model.AppInfo
import com.simonslauncher.app.ui.theme.TextPrimary

/** 16:9 like Android TV banners. 7 fit on screen, the 8th peeks in to show the row scrolls. */
val TILE_WIDTH = 118.dp
val TILE_HEIGHT = 66.dp
private val TILE_RADIUS = 11.dp
private val TileShape = RoundedCornerShape(TILE_RADIUS)
private val LabelStyle = TextStyle(fontSize = 13.sp, color = TextPrimary, textAlign = TextAlign.Center)

@Composable
fun AppTile(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    moving: Boolean,
    onMove: (Int) -> Unit,
    onMoveDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focus = animateFocus(isFocused)

    Column(
        modifier = Modifier.width(TILE_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(TILE_HEIGHT)
                .focusGlow(
                    progress = { focus.value },
                    shape = TileShape,
                    cornerRadius = TILE_RADIUS,
                    focusScale = 1.08f
                )
                .then(if (moving) Modifier.moveKeys(onMove, onMoveDone) else Modifier)
                .tvClickable(
                    onFocusChange = {
                        isFocused = it
                        if (it) onFocused()
                    },
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AppBanner(app.packageName, app.label, iconSize = 40.dp)
            if (moving) MoveArrows()
        }
        Spacer(Modifier.height(10.dp))
        // Only the focused tile shows its name: the logos already say which app it is.
        BasicText(
            text = if (moving) "◀  ▶   OK" else app.label,
            style = LabelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer { alpha = focus.value }
        )
    }
}

/** While moving: left/right shift the tile, OK or Back drop it. Consumed so focus stays put. */
private fun Modifier.moveKeys(onMove: (Int) -> Unit, onMoveDone: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent event.key in MOVE_KEYS
        when (event.key) {
            Key.DirectionLeft -> onMove(-1)
            Key.DirectionRight -> onMove(1)
            Key.DirectionCenter, Key.Enter, Key.Back -> onMoveDone()
            Key.DirectionUp, Key.DirectionDown -> Unit
            else -> return@onPreviewKeyEvent false
        }
        true
    }

private val MOVE_KEYS = setOf(
    Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown,
    Key.DirectionCenter, Key.Enter, Key.Back
)

@Composable
private fun BoxScope.MoveArrows() {
    Box(Modifier.matchParentSize().background(Color(0x66000000)))
    for ((icon, align) in listOf(Icons.AutoMirrored.Filled.KeyboardArrowLeft to Alignment.CenterStart, Icons.AutoMirrored.Filled.KeyboardArrowRight to Alignment.CenterEnd)) {
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Amber),
            modifier = Modifier.align(align).size(30.dp)
        )
    }
}
