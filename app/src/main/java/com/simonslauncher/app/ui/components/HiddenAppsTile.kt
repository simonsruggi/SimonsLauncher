package com.simonslauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.ui.brand.IconTileBrush
import com.simonslauncher.app.ui.theme.Text
import com.simonslauncher.app.ui.theme.TextSecondary

private val RADIUS = 11.dp
private val Shape = RoundedCornerShape(RADIUS)

/** Last tile of the row while some apps are hidden: opens the list to put them back. */
@Composable
fun HiddenAppsTile(count: Int, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val focus = animateFocus(isFocused)
    Column(Modifier.width(TILE_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TILE_HEIGHT)
                .focusGlow(progress = { focus.value }, shape = Shape, cornerRadius = RADIUS, focusScale = 1.08f)
                .background(IconTileBrush)
                .tvClickable(onFocusChange = { isFocused = it }, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", fontSize = 22.sp, fontWeight = FontWeight.Medium)
                Text("nascoste", fontSize = 12.sp, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
