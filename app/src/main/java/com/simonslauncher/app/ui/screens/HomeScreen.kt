package com.simonslauncher.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simonslauncher.app.LauncherViewModel
import com.simonslauncher.app.model.AppInfo
import com.simonslauncher.app.ui.components.AppMenu
import com.simonslauncher.app.ui.components.AppRow
import com.simonslauncher.app.ui.components.Backdrop
import com.simonslauncher.app.ui.components.BackdropSource
import com.simonslauncher.app.ui.components.HiddenAppsPanel
import com.simonslauncher.app.ui.components.SourcesRow
import com.simonslauncher.app.ui.components.Spotlight
import com.simonslauncher.app.ui.components.TopBar
import com.simonslauncher.app.ui.components.tintFor

@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val spotlight by viewModel.spotlight.collectAsStateWithLifecycle()
    val homePresses by viewModel.homePresses.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    // The page slides up only while the sources row (below the screen) has focus. A scroll
    // container would also scroll on focus moves inside the app row, hiding the top bar.
    var sourcesFocused by remember { mutableStateOf(false) }
    var pageHeight by remember { mutableIntStateOf(0) }

    // Read only inside Backdrop: moving focus never recomposes the whole screen.
    val backdrop = remember { mutableStateOf<BackdropSource>(BackdropSource.Plain) }
    val spotlightArt = spotlight.firstOrNull()?.artUrl
    LaunchedEffect(spotlightArt) {
        if (backdrop.value == BackdropSource.Plain && spotlightArt != null) {
            backdrop.value = BackdropSource.Art(spotlightArt)
        }
    }

    var menuFor by remember { mutableStateOf<AppInfo?>(null) }
    var moving by remember { mutableStateOf<String?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    // Package whose tile should take focus (launch, after a menu, after Home).
    var focusTarget by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // The focus placed on the first tile at launch is not a choice: the spotlight keeps the mix.
    var userMoved by remember { mutableStateOf(false) }
    val onAppFocused = remember(viewModel) {
        { app: AppInfo ->
            if (userMoved) {
                viewModel.focusApp(app.packageName)
                val art = viewModel.programsFor(app.packageName).firstOrNull()?.artUrl
                backdrop.value = art?.let { BackdropSource.Art(it) } ?: tintFor(app.brand)
            }
            userMoved = true
        }
    }

    var initialFocusDone by remember { mutableStateOf(false) }
    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty() && !initialFocusDone) {
            focusTarget = apps.first().packageName
            initialFocusDone = true
        }
    }
    // Home pressed while already home: close everything, back to the first tile and the mix.
    LaunchedEffect(homePresses) {
        if (homePresses == 0) return@LaunchedEffect
        menuFor = null
        moving = null
        showHidden = false
        userMoved = false
        spotlightArt?.let { backdrop.value = BackdropSource.Art(it) }
        listState.scrollToItem(0)
        focusTarget = apps.firstOrNull()?.packageName
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = constraints.maxHeight
        val pageShift = animateFloatAsState(
            if (sourcesFocused) (pageHeight - screenHeight).coerceAtLeast(0).toFloat() else 0f,
            tween(260),
            label = "pageShift"
        )
        Backdrop(source = { backdrop.value }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .onSizeChanged { pageHeight = it.height }
                .graphicsLayer { translationY = -pageShift.value }
                .padding(top = 18.dp)
        ) {
            TopBar(clock = clock, weather = weather, onOpenSettings = { viewModel.openSettings() })

            Spacer(Modifier.height(18.dp))

            Spotlight(
                programs = spotlight,
                onOpen = { viewModel.openProgram(it) },
                onFocused = { program -> program.artUrl?.let { backdrop.value = BackdropSource.Art(it) } },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .height(326.dp)
            )

            Spacer(Modifier.height(14.dp))

            AppRow(
                apps = apps,
                hiddenCount = hiddenApps.size,
                listState = listState,
                moving = moving,
                focusTarget = focusTarget,
                onFocusTargetReached = { focusTarget = null },
                onAppClick = { viewModel.launchApp(it) },
                onAppLongClick = { if (moving == null) menuFor = it },
                onAppFocused = onAppFocused,
                onMove = { pkg, delta -> viewModel.move(pkg, delta) },
                onMoveDone = { moving = null },
                onOpenHidden = { showHidden = true }
            )

            SourcesRow(
                sources = sources,
                onOpen = { viewModel.openSource(it) },
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 24.dp)
                    .onFocusChanged { sourcesFocused = it.hasFocus }
            )
        }

        menuFor?.let { app ->
            AppMenu(
                app = app,
                onMove = {
                    menuFor = null
                    moving = app.packageName
                    focusTarget = app.packageName
                },
                onHide = {
                    menuFor = null
                    val row = apps.map { it.packageName }
                    val next = row.getOrNull(row.indexOf(app.packageName) + 1) ?: row.getOrNull(row.indexOf(app.packageName) - 1)
                    viewModel.hide(app.packageName)
                    focusTarget = next
                },
                onDismiss = {
                    menuFor = null
                    focusTarget = app.packageName
                }
            )
        }

        if (showHidden) {
            HiddenAppsPanel(
                apps = hiddenApps,
                onRestore = { app ->
                    viewModel.unhide(app.packageName)
                    if (hiddenApps.size <= 1) {
                        showHidden = false
                        focusTarget = app.packageName
                    }
                },
                onDismiss = {
                    showHidden = false
                    focusTarget = apps.lastOrNull()?.packageName
                }
            )
        }
    }
}
