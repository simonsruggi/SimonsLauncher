package com.simonslauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.model.TvSource
import com.simonslauncher.app.ui.brand.IconTileBrush
import com.simonslauncher.app.ui.theme.Text
import com.simonslauncher.app.ui.theme.TextMuted
import com.simonslauncher.app.ui.theme.TextSecondary

private val RADIUS = 11.dp
private val Shape = RoundedCornerShape(RADIUS)
private val DeviceStyle = TextStyle(fontSize = 11.sp, color = TextSecondary)

/** Second row, below the apps (reached by scrolling down): the TV's HDMI/AV inputs. */
@Composable
fun SourcesRow(sources: List<TvSource>, onOpen: (TvSource) -> Unit, modifier: Modifier = Modifier) {
    if (sources.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Sorgenti", fontSize = 15.sp, color = TextSecondary, modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 21.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sources, key = { it.inputId }) { source -> SourceTile(source, onClick = { onOpen(source) }) }
        }
    }
}

@Composable
private fun SourceTile(source: TvSource, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val focus = animateFocus(isFocused)
    Box(
        modifier = Modifier
            .width(TILE_WIDTH)
            .height(TILE_HEIGHT)
            .focusGlow(progress = { focus.value }, shape = Shape, cornerRadius = RADIUS, focusScale = 1.08f)
            .background(IconTileBrush)
            .tvClickable(onFocusChange = { isFocused = it }, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Nothing plugged in: still selectable, just dimmed.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { alpha = if (source.connected) 1f else 0.45f }
        ) {
            Text(source.label, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            BasicText(
                text = source.deviceName ?: if (source.connected) "" else "Scollegato",
                style = if (source.deviceName != null) DeviceStyle else DeviceStyle.copy(color = TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
