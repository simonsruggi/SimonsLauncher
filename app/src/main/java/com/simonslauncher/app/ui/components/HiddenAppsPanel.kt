package com.simonslauncher.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.simonslauncher.app.model.AppInfo

/** Hidden apps; OK puts one back in the row. */
@Composable
fun HiddenAppsPanel(
    apps: List<AppInfo>,
    onRestore: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    OverlayPanel(title = "App nascoste", onDismiss = onDismiss) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                MenuButton(
                    label = "Mostra ${app.label}",
                    onClick = { onRestore(app) },
                    modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                    leading = {
                        AppBanner(
                            packageName = app.packageName,
                            label = app.label,
                            iconSize = 22.dp,
                            modifier = Modifier.size(width = 56.dp, height = 32.dp).clip(RoundedCornerShape(6.dp))
                        )
                    }
                )
            }
        }
    }
}
