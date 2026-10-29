package com.simonslauncher.app.util

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import com.simonslauncher.app.model.TvSource

/** HDMI and AV inputs from TvInputManager; opening one hands the switch to the TV app. */
object TvSources {

    private val PHYSICAL = setOf(
        TvInputInfo.TYPE_HDMI,
        TvInputInfo.TYPE_COMPONENT,
        TvInputInfo.TYPE_COMPOSITE,
        TvInputInfo.TYPE_SCART,
        TvInputInfo.TYPE_SVIDEO,
        TvInputInfo.TYPE_VGA,
        TvInputInfo.TYPE_DVI,
        TvInputInfo.TYPE_DISPLAY_PORT
    )

    fun load(context: Context): List<TvSource> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return try {
            val inputs = manager.tvInputList
            // HDMI-CEC devices appear as child inputs of their port: their name labels the port.
            val children = inputs.filter { it.parentId != null }.groupBy { it.parentId }
            inputs
                .filter { it.type in PHYSICAL && it.parentId == null && !it.isHidden(context) }
                .map { info ->
                    val label = info.loadCustomLabel(context)?.toString()?.takeIf { it.isNotBlank() }
                        ?: info.loadLabel(context).toString()
                    TvSource(
                        inputId = info.id,
                        label = label,
                        deviceName = children[info.id]?.firstOrNull()?.loadLabel(context)?.toString(),
                        connected = manager.getInputState(info.id) != TvInputManager.INPUT_STATE_DISCONNECTED
                    )
                }
                .sortedWith(compareBy({ !it.label.startsWith("HDMI", ignoreCase = true) }, { it.label }))
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun intentFor(source: TvSource): Intent =
        Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(source.inputId))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
