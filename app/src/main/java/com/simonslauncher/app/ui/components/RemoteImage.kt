package com.simonslauncher.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.simonslauncher.app.util.ImageStore
import com.simonslauncher.app.util.RemoteArt

private const val FADE_IN_MS = 260

/** Artwork from the apps, loaded off the main thread; cached art shows on the first frame. */
@Composable
fun RemoteImage(
    url: String?,
    widthPx: Int,
    modifier: Modifier = Modifier,
    backdrop: Boolean = false,
    placeholder: Color? = null,
    fadeIn: Boolean = true
) {
    // A new image must restart loading and fade: produceState alone keeps the previous bitmap.
    key(url) { RemoteImageContent(url, widthPx, modifier, backdrop, placeholder, fadeIn) }
}

@Composable
private fun RemoteImageContent(
    url: String?,
    widthPx: Int,
    modifier: Modifier,
    backdrop: Boolean,
    placeholder: Color?,
    fadeIn: Boolean
) {
    if (url == null || ImageStore.released) {
        placeholder?.let { Box(modifier.background(it)) }
        return
    }
    val context = LocalContext.current
    val wasCached = remember(url) { RemoteArt.peek(url, widthPx, backdrop) != null }
    val alpha = remember(url) { Animatable(if (wasCached || !fadeIn) 1f else 0f) }
    val bitmap by produceState(RemoteArt.peek(url, widthPx, backdrop), url) {
        if (value == null) value = RemoteArt.load(context, url, widthPx, backdrop)
    }
    LaunchedEffect(bitmap) {
        if (bitmap != null) alpha.animateTo(1f, tween(FADE_IN_MS))
    }
    val image = bitmap ?: run {
        placeholder?.let { Box(modifier.background(it)) }
        return
    }
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.graphicsLayer { this.alpha = alpha.value }
    )
}
