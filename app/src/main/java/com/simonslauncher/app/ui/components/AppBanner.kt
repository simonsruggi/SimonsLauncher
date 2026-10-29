package com.simonslauncher.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.ui.brand.IconTileBrush
import com.simonslauncher.app.ui.theme.Text
import com.simonslauncher.app.util.AppLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The app's official TV banner (what every Android TV app ships for launchers), filling a
 * 16:9 box. Apps without one get their icon on a dark tile.
 */
@Composable
fun AppBanner(packageName: String, label: String, iconSize: Dp, modifier: Modifier = Modifier) {
    // A different app must reload: produceState alone would keep the previous banner.
    key(packageName) { AppBannerContent(packageName, label, iconSize, modifier) }
}

@Composable
private fun AppBannerContent(packageName: String, label: String, iconSize: Dp, modifier: Modifier) {
    val context = LocalContext.current
    val art by produceState(AppLoader.peekTileArt(packageName), packageName) {
        if (value == null) value = withContext(Dispatchers.IO) { AppLoader.loadTileArt(context, packageName) }
    }
    val current = art
    if (current?.isBanner == true) {
        Image(current.image, label, modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        return
    }
    Box(modifier.fillMaxSize().background(IconTileBrush), contentAlignment = Alignment.Center) {
        if (current != null) {
            Image(current.image, label, Modifier.size(iconSize))
        } else {
            Text(label.take(1), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
