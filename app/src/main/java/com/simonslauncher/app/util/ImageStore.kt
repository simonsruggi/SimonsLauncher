package com.simonslauncher.app.util

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * Decodes the launcher artwork off the main thread and keeps it for instant returns to Home.
 * State is touched on the main thread only; decoding runs on IO, at most once per image.
 */
object ImageStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cache = HashMap<Int, ImageBitmap>()
    private val inFlight = HashMap<Int, Deferred<ImageBitmap>>()

    /** True after Android asked for memory while Home was hidden: images are not drawn. */
    var released by mutableStateOf(false)
        private set

    fun peek(@DrawableRes id: Int): ImageBitmap? = cache[id]

    suspend fun load(context: Context, @DrawableRes id: Int): ImageBitmap {
        cache[id]?.let { return it }
        val app = context.applicationContext
        val job = inFlight.getOrPut(id) {
            scope.async(Dispatchers.IO) {
                BitmapFactory.decodeResource(app.resources, id).asImageBitmap().also { it.prepareToDraw() }
            }
        }
        val bitmap = job.await()
        inFlight.remove(id)
        if (!released) cache[id] = bitmap
        return bitmap
    }

    fun release() {
        cache.clear()
        released = true
    }

    fun restore() {
        released = false
    }
}
