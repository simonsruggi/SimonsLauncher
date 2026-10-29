package com.simonslauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.model.Program
import com.simonslauncher.app.ui.theme.Surface
import com.simonslauncher.app.ui.theme.Text

private val LiveRed = Color(0xFFE5203A)

/** A title's real artwork, shown whole: the apps' art already carries the title and logo. */
@Composable
fun SpotlightCard(
    program: Program?,
    widthPx: Int,
    radius: Dp,
    focusScale: Float,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focus = animateFocus(isFocused)
    val shape = remember(radius) { RoundedCornerShape(radius) }

    Box(
        modifier = modifier
            .focusGlow(
                progress = { focus.value },
                shape = shape,
                cornerRadius = radius,
                focusScale = focusScale,
                idleElevation = 6.dp,
                focusElevation = 14.dp,
                idleBorder = { Color(0x1FFFFFFF) }
            )
            .tvClickable(
                onFocusChange = {
                    isFocused = it
                    if (it) onFocused()
                },
                onClick = onClick
            )
    ) {
        RemoteImage(
            url = program?.artUrl,
            widthPx = widthPx,
            placeholder = Surface,
            modifier = Modifier.fillMaxSize()
        )
        if (program?.live == true) {
            Text(
                text = "IN DIRETTA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(LiveRed, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
