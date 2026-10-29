package com.simonslauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.ui.theme.Text
import com.simonslauncher.app.ui.theme.TextPrimary

private val MENU_RADIUS = 10.dp
private val MenuShape = RoundedCornerShape(MENU_RADIUS)

/** A row in the launcher's overlay menus: gold frame on focus, like every other control. */
@Composable
fun MenuButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val focus = animateFocus(isFocused)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .focusGlow(
                progress = { focus.value },
                shape = MenuShape,
                cornerRadius = MENU_RADIUS,
                focusScale = 1.02f,
                idleBorder = { Color.Transparent },
                idleBorderWidth = 0.dp,
                focusBorderWidth = 2.dp
            )
            // Opaque: a translucent fill would let the gold shadow show through as a dark band.
            .background(Color(0xFF23232E))
            .tvClickable(onFocusChange = { isFocused = it }, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Text(label, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.padding(start = if (leading != null) 12.dp else 0.dp))
    }
}
