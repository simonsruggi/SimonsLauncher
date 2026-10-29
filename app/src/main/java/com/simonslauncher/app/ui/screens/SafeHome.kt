package com.simonslauncher.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.model.AppInfo
import com.simonslauncher.app.ui.components.MenuButton
import com.simonslauncher.app.ui.theme.Background
import com.simonslauncher.app.ui.theme.Text
import com.simonslauncher.app.util.AppLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shown after the launcher crashed twice at start: no titles, artwork, weather or network,
 * only the installed apps, so the TV always has a working Home.
 */
@Composable
fun SafeHome(onRetry: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val apps by produceState(emptyList<AppInfo>()) {
        value = withContext(Dispatchers.IO) { AppLoader.loadApps(context) }
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(Modifier.fillMaxSize().background(Background).padding(40.dp)) {
        Text("Modalità sicura", fontSize = 26.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        MenuButton("Riprova il launcher completo", onRetry, Modifier.width(420.dp).focusRequester(first))
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(420.dp)) {
            items(apps, key = { it.packageName }) { app -> MenuButton(app.label, { onOpen(app.packageName) }) }
        }
    }
}
