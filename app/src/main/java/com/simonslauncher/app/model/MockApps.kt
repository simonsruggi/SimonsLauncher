package com.simonslauncher.app.model

object MockApps {
    fun get(): List<AppInfo> = Brand.entries.map { AppInfo(it.packages.first(), it.displayName) }
}
