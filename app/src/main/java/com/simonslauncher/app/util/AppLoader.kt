package com.simonslauncher.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.simonslauncher.app.model.AppInfo
import com.simonslauncher.app.model.SETTINGS_PACKAGES

object AppLoader {

    private const val ICON_PX = 96
    private const val BANNER_W_PX = 320
    private const val BANNER_H_PX = 180

    fun loadApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val ownPackage = context.packageName

        val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val regularIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val seen = mutableSetOf<String>()
        val apps = mutableListOf<AppInfo>()

        val allResolves = mutableListOf<ResolveInfo>()
        allResolves.addAll(pm.queryIntentActivities(leanbackIntent, 0))
        allResolves.addAll(pm.queryIntentActivities(regularIntent, 0))

        for (ri in allResolves) {
            val pkg = ri.activityInfo.packageName
            if (pkg == ownPackage || pkg in SETTINGS_PACKAGES || !seen.add(pkg)) continue
            val app = AppInfo(packageName = pkg, label = ri.loadLabel(pm).toString())
            apps.add(app.copy(label = app.brand?.displayName ?: app.label))
        }
        return sorted(apps)
    }

    fun sorted(apps: List<AppInfo>): List<AppInfo> =
        apps.sortedWith(compareBy<AppInfo> { it.brand?.ordinal ?: Int.MAX_VALUE }.thenBy { it.label.lowercase() })

    /** The app's own art for the launcher: its official 16:9 TV banner, else its icon. */
    data class TileArt(val image: ImageBitmap, val isBanner: Boolean)

    private val art = LruCache<String, TileArt>(64)

    fun peekTileArt(packageName: String): TileArt? = art.get(packageName)

    fun loadTileArt(context: Context, packageName: String): TileArt? {
        art.get(packageName)?.let { return it }
        val pm = context.packageManager
        return try {
            val banner = pm.getApplicationBanner(packageName)
                ?: pm.getLeanbackLaunchIntentForPackage(packageName)?.component?.let { pm.getActivityBanner(it) }
            val result = if (banner != null) {
                TileArt(banner.toBitmap(BANNER_W_PX, BANNER_H_PX).asImageBitmap().also { it.prepareToDraw() }, isBanner = true)
            } else {
                TileArt(pm.getApplicationIcon(packageName).toBitmap(ICON_PX, ICON_PX).asImageBitmap(), isBanner = false)
            }
            art.put(packageName, result)
            result
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getLaunchIntent(context: Context, packageName: String): Intent? {
        val pm = context.packageManager
        return pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
    }
}
