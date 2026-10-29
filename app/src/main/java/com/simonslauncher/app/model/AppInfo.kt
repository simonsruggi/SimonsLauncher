package com.simonslauncher.app.model

data class AppInfo(
    val packageName: String,
    val label: String,
    val brand: Brand? = Brand.of(packageName)
)

/** Settings live in the top bar, not in the app row. */
val SETTINGS_PACKAGES = setOf(
    "com.android.tv.settings",
    "com.android.settings",
    "com.sony.dtv.configsettings"
)
