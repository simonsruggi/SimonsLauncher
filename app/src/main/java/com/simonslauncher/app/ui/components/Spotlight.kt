package com.simonslauncher.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.model.Program
import com.simonslauncher.app.ui.theme.TextPrimary
import com.simonslauncher.app.ui.theme.TextSecondary

private val TitleStyle = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Medium, color = TextPrimary, lineHeight = 26.sp)
private val DescriptionStyle = TextStyle(fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)

// Drawn sizes in px at 320 dpi, so the CDNs are asked for exactly what fits.
private const val MAIN_PX = 1160
private const val SMALL_PX = 400

/**
 * Real titles from the apps: big 16:9 artwork on the left, the focused title's details on the
 * right above the next two titles.
 */
@Composable
fun Spotlight(
    programs: List<Program>,
    onOpen: (Program) -> Unit,
    onFocused: (Program) -> Unit,
    modifier: Modifier = Modifier
) {
    val main = programs.getOrNull(0)
    var detail by remember { mutableStateOf<Program?>(null) }
    val shown = detail?.takeIf { it in programs } ?: main

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        SpotlightCard(
            program = main,
            widthPx = MAIN_PX,
            radius = 16.dp,
            focusScale = 1.015f,
            onClick = { main?.let(onOpen) },
            onFocused = {
                detail = main
                main?.let(onFocused)
            },
            modifier = Modifier.fillMaxHeight().aspectRatio(16f / 9f)
        )
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp, top = 4.dp)) {
                shown?.let {
                    AppBanner(
                        packageName = it.packageName,
                        label = it.brand?.displayName.orEmpty(),
                        iconSize = 28.dp,
                        modifier = Modifier.size(width = 80.dp, height = 45.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                Spacer(Modifier.height(14.dp))
                BasicText(
                    text = shown?.title.orEmpty(),
                    style = TitleStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = shown?.description.orEmpty(),
                    style = DescriptionStyle,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 1..2) {
                    val program = programs.getOrNull(i)
                    SpotlightCard(
                        program = program,
                        widthPx = SMALL_PX,
                        radius = 12.dp,
                        focusScale = 1.04f,
                        onClick = { program?.let(onOpen) },
                        onFocused = {
                            detail = program
                            program?.let(onFocused)
                        },
                        modifier = Modifier.weight(1f).aspectRatio(16f / 9f)
                    )
                }
            }
        }
    }
}
