package com.simonslauncher.app.ui.components

import androidx.annotation.DrawableRes
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

private const val FADE_IN_MS = 220

/** Artwork decoded in background: the first frame never waits for it. Cached images show at once. */
@Composable
fun AsyncImage(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Color? = null,
    fadeIn: Boolean = true
) {
    // A new image must restart loading and fade: produceState alone keeps the previous bitmap.
    key(id) { AsyncImageContent(id, modifier, contentScale, placeholder, fadeIn) }
}

@Composable
private fun AsyncImageContent(
    @DrawableRes id: Int,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholder: Color?,
    fadeIn: Boolean
) {
    if (ImageStore.released) {
        placeholder?.let { Box(modifier.background(it)) }
        return
    }
    val context = LocalContext.current
    val wasCached = remember(id) { ImageStore.peek(id) != null }
    val alpha = remember(id) { Animatable(if (wasCached || !fadeIn) 1f else 0f) }
    val bitmap by produceState(ImageStore.peek(id), id) {
        if (value == null) value = ImageStore.load(context, id)
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
        contentScale = contentScale,
        modifier = modifier.graphicsLayer { this.alpha = alpha.value }
    )
}
