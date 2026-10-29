package com.simonslauncher.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.simonslauncher.app.R
import com.simonslauncher.app.model.Brand
import com.simonslauncher.app.ui.theme.Background
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** What fills the screen behind everything: the focused title's art, or the app's color. */
sealed interface BackdropSource {
    data class Art(val url: String) : BackdropSource
    data class Tint(@DrawableRes val res: Int) : BackdropSource
    data object Plain : BackdropSource
}

/** Opaque brand-tint images with the shade baked in, for apps that publish no titles. */
fun tintFor(brand: Brand?): BackdropSource = when (brand) {
    Brand.YOUTUBE -> BackdropSource.Tint(R.drawable.bg_youtube)
    Brand.DISNEY -> BackdropSource.Tint(R.drawable.bg_disney)
    Brand.RAIPLAY -> BackdropSource.Tint(R.drawable.bg_raiplay)
    Brand.NOW -> BackdropSource.Tint(R.drawable.bg_now)
    else -> BackdropSource.Plain
}

/** Waits for the focus to settle, so scrolling fast across tiles doesn't stack fades. */
private const val SETTLE_MS = 160L
private const val FADE_MS = 560

@Composable
fun Backdrop(source: () -> BackdropSource, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(source()) }
    LaunchedEffect(Unit) {
        snapshotFlow(source).collectLatest {
            delay(SETTLE_MS)
            shown = it
        }
    }
    Crossfade(targetState = shown, modifier = modifier, animationSpec = tween(FADE_MS), label = "backdrop") { current ->
        val fill = Modifier.fillMaxSize()
        when (current) {
            is BackdropSource.Art -> RemoteImage(current.url, 0, fill, backdrop = true, placeholder = Background, fadeIn = false)
            is BackdropSource.Tint -> AsyncImage(current.res, fill, placeholder = Background, fadeIn = false)
            BackdropSource.Plain -> Box(fill.background(Background))
        }
    }
}
