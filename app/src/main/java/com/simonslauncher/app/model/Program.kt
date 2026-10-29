package com.simonslauncher.app.model

/** A title an app publishes on its Android TV preview channel (real artwork and deep link). */
data class Program(
    val id: Long,
    val packageName: String,
    val title: String,
    val description: String?,
    val artUrl: String?,
    val live: Boolean,
    val intentUri: String?
) {
    val brand: Brand? get() = Brand.of(packageName)
}
