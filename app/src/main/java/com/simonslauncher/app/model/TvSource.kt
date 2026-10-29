package com.simonslauncher.app.model

/** A physical input of the TV (HDMI, AV), with the name of the device plugged in when known. */
data class TvSource(
    val inputId: String,
    val label: String,
    val deviceName: String?,
    val connected: Boolean
)
