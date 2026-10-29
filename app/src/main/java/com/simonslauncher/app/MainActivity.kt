package com.simonslauncher.app

import android.content.ComponentCallbacks2
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.simonslauncher.app.ui.screens.HomeScreen
import com.simonslauncher.app.ui.screens.SafeHome
import com.simonslauncher.app.util.AppLoader
import com.simonslauncher.app.util.CrashGuard
import com.simonslauncher.app.util.ImageStore
import com.simonslauncher.app.util.RemoteArt
import com.simonslauncher.app.util.UiSound

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private var safeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safeMode = CrashGuard.inCrashLoop(this)
        if (safeMode) {
            setContent {
                SafeHome(
                    onRetry = {
                        CrashGuard.clear(this)
                        recreate()
                    },
                    onOpen = { pkg -> AppLoader.getLaunchIntent(this, pkg)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                )
            }
            return
        }
        if (checkSelfPermission(READ_TV_LISTINGS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(READ_TV_LISTINGS), REQUEST_TV_LISTINGS)
        }

        setContent {
            HomeScreen(viewModel = viewModel)
        }
        // The window background only avoids a flash at launch; afterwards the opaque
        // backdrop covers it, so drawing it would cost a full-screen layer per frame.
        window.decorView.post { window.setBackgroundDrawable(null) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        UiSound.armed = true
        return super.dispatchKeyEvent(event)
    }

    /** Home pressed while the launcher is already in front (singleTask). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!safeMode && intent.hasCategory(Intent.CATEGORY_HOME)) viewModel.onHomePressed()
    }

    override fun onStart() {
        super.onStart()
        if (safeMode) return
        ImageStore.restore()
        viewModel.refreshContent()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_TV_LISTINGS) viewModel.refreshContent()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Home is hidden and the system is short on memory: give the artwork back
        // instead of risking the whole process being killed. It reloads on return.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            ImageStore.release()
            RemoteArt.trimMemory()
        }
    }

    private companion object {
        const val REQUEST_TV_LISTINGS = 1
        // Constant added to Manifest.permission only in API 30; the permission exists since 26.
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
    }
}
