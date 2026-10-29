package com.simonslauncher.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.simonslauncher.app.ui.theme.TextPrimary

private val SIZE = 40.dp

/** Opens the TV settings; sits in the top bar next to clock and weather. */
@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val focus = animateFocus(isFocused)
    Box(
        modifier = modifier
            .size(SIZE)
            .focusGlow(
                progress = { focus.value },
                shape = CircleShape,
                cornerRadius = SIZE / 2,
                focusScale = 1.12f,
                idleBorder = { Color.Transparent },
                idleBorderWidth = 0.dp,
                focusBorderWidth = 2.dp
            )
            .tvClickable(onFocusChange = { isFocused = it }, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Impostazioni",
            colorFilter = ColorFilter.tint(TextPrimary),
            modifier = Modifier.size(26.dp)
        )
    }
}
