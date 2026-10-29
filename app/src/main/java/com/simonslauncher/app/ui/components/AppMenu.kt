package com.simonslauncher.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.simonslauncher.app.model.AppInfo

/** Long press OK on an app: move it along the row or hide it. */
@Composable
fun AppMenu(
    app: AppInfo,
    onMove: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    OverlayPanel(title = app.label, onDismiss = onDismiss) {
        MenuButton("Sposta", onMove, Modifier.focusRequester(first))
        Spacer(Modifier.height(8.dp))
        MenuButton("Nascondi", onHide)
        Spacer(Modifier.height(8.dp))
        MenuButton("Annulla", onDismiss)
    }
}
