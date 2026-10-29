package com.simonslauncher.app.util

import android.content.Context
import android.media.AudioManager

/**
 * The TV's own navigation and select sounds, so the launcher sounds like the rest of Android TV.
 * Silent until the first key press: the focus placed automatically at launch makes no sound.
 */
object UiSound {
    @Volatile
    var armed = false

    fun move(context: Context) = play(context, AudioManager.FX_FOCUS_NAVIGATION_RIGHT)

    fun select(context: Context) = play(context, AudioManager.FX_KEY_CLICK)

    private fun play(context: Context, effect: Int) {
        if (!armed) return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // The volume overload plays even when "system sounds" are off in the TV settings:
        // the sound was asked for explicitly. -1 = media volume minus 3 dB.
        audio.playSoundEffect(effect, -1f)
    }
}
