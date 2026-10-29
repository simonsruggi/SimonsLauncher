package com.simonslauncher.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.simonslauncher.app.model.AppInfo

/** Fixed height: the row must not jump while the app list is still loading. */
private val ROW_HEIGHT = TILE_HEIGHT + 48.dp
const val HIDDEN_TILE_KEY = "#hidden"

@Composable
fun AppRow(
    apps: List<AppInfo>,
    hiddenCount: Int,
    listState: LazyListState,
    moving: String?,
    focusTarget: String?,
    onFocusTargetReached: () -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onAppFocused: (AppInfo) -> Unit,
    onMove: (String, Int) -> Unit,
    onMoveDone: () -> Unit,
    onOpenHidden: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT),
        contentPadding = PaddingValues(horizontal = 21.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { _, app ->
            val requester = remember { FocusRequester() }
            if (focusTarget == app.packageName) {
                LaunchedEffect(focusTarget) {
                    runCatching { requester.requestFocus() }
                    onFocusTargetReached()
                }
            }
            AppTile(
                app = app,
                onClick = { onAppClick(app.packageName) },
                onLongClick = { onAppLongClick(app) },
                onFocused = { onAppFocused(app) },
                moving = moving == app.packageName,
                onMove = { delta -> onMove(app.packageName, delta) },
                onMoveDone = onMoveDone,
                modifier = Modifier.focusRequester(requester)
            )
        }
        if (hiddenCount > 0) {
            item(key = HIDDEN_TILE_KEY) {
                HiddenAppsTile(count = hiddenCount, onClick = onOpenHidden)
            }
        }
    }
}
