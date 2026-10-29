package com.simonslauncher.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.simonslauncher.app.util.UiSound
import com.simonslauncher.app.ui.theme.Amber

const val FOCUS_ANIM_MS = 170

/**
 * D-pad focus + OK/Enter press, with the TV's navigation and select sounds.
 * With [onLongClick], OK fires on release and holding it (first key repeat, ~0.5 s) opens
 * the long-press action instead; without it, OK fires on press for the snappiest response.
 */
@Composable
fun Modifier.tvClickable(
    onFocusChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier {
    val context = LocalContext.current
    // [0] = this element saw the OK press, [1] = the hold already fired the long-press action.
    // The press check matters: an OK pressed on a menu button can be released after focus has
    // moved here (e.g. "Nascondi"), and that release must not open the app.
    val okState = remember { BooleanArray(2) }
    return this
        .onFocusChanged {
            if (it.isFocused) UiSound.move(context)
            if (!it.isFocused) okState[0] = false
            onFocusChange(it.isFocused)
        }
        .onKeyEvent { event ->
            if (event.key != Key.DirectionCenter && event.key != Key.Enter && event.key != Key.NumPadEnter) {
                return@onKeyEvent false
            }
            val repeat = event.nativeKeyEvent.repeatCount
            when {
                event.type == KeyEventType.KeyDown && repeat == 0 -> {
                    okState[0] = true
                    okState[1] = false
                    if (onLongClick == null) {
                        UiSound.select(context)
                        onClick()
                    }
                }
                event.type == KeyEventType.KeyDown && onLongClick != null && okState[0] && !okState[1] -> {
                    okState[1] = true
                    UiSound.select(context)
                    onLongClick()
                }
                event.type == KeyEventType.KeyUp -> {
                    if (onLongClick != null && okState[0] && !okState[1]) {
                        UiSound.select(context)
                        onClick()
                    }
                    okState[0] = false
                }
            }
            true
        }
        .focusable()
}

@Composable
fun animateFocus(focused: Boolean): State<Float> =
    animateFloatAsState(if (focused) 1f else 0f, tween(FOCUS_ANIM_MS), label = "focus")

/**
 * Scale, gold glow and gold border driven by [progress] (0 = idle, 1 = focused).
 * Values are read only inside layer/draw lambdas, so an animation frame never recomposes.
 */
fun Modifier.focusGlow(
    progress: () -> Float,
    shape: Shape,
    cornerRadius: Dp,
    focusScale: Float,
    idleElevation: Dp = 0.dp,
    focusElevation: Dp = 12.dp,
    idleBorder: () -> Color = { Color(0x1AFFFFFF) },
    idleBorderWidth: Dp = 1.dp,
    focusBorderWidth: Dp = 2.5.dp,
    idleGlow: () -> Float = { 0f }
): Modifier = this
    .graphicsLayer {
        val p = progress()
        val glow = maxOf(p, idleGlow())
        val s = 1f + (focusScale - 1f) * p
        scaleX = s
        scaleY = s
        this.shape = shape
        clip = true
        shadowElevation = (idleElevation + (focusElevation - idleElevation) * glow).toPx()
        val shadow = lerp(Color.Black, Amber, glow)
        ambientShadowColor = shadow
        spotShadowColor = shadow
    }
    .drawWithContent {
        drawContent()
        val p = progress()
        val width = (idleBorderWidth + (focusBorderWidth - idleBorderWidth) * p).toPx()
        val color = lerp(idleBorder(), Amber, p)
        if (color.alpha == 0f) return@drawWithContent
        val r = cornerRadius.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(width / 2, width / 2),
            size = Size(size.width - width, size.height - width),
            cornerRadius = CornerRadius(r - width / 2),
            style = Stroke(width)
        )
    }

