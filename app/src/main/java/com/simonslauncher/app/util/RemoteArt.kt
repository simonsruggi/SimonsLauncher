package com.simonslauncher.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Artwork published by the apps: downloaded once (disk cache in cacheDir, pruned after a few
 * days), decoded at the size it is drawn, kept in a small memory cache.
 */
object RemoteArt {
    private const val MEMORY_BYTES = 10 * 1024 * 1024
    private const val DISK_MAX_AGE_MS = 3L * 24 * 60 * 60 * 1000
    private const val BACKDROP_W = 96
    private const val SMALL_FETCH_W = 640

    private val memory = object : LruCache<String, ImageBitmap>(MEMORY_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.asAndroidBitmap().byteCount
    }

    fun peek(url: String, widthPx: Int, backdrop: Boolean = false): ImageBitmap? = memory.get(key(url, widthPx, backdrop))

    suspend fun load(context: Context, url: String, widthPx: Int, backdrop: Boolean = false): ImageBitmap? {
        val key = key(url, widthPx, backdrop)
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bytes = fetch(context, sizedUrl(url, if (backdrop) SMALL_FETCH_W else widthPx)) ?: return@withContext null
            val bitmap = if (backdrop) decodeBackdrop(bytes) else decode(bytes, widthPx)
            bitmap?.asImageBitmap()?.also {
                it.prepareToDraw()
                memory.put(key, it)
            }
        }
    }

    fun trimMemory() = memory.evictAll()

    private val placeholderVerdicts = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /**
     * Some apps publish a bare logo on white instead of real art (DAZN's 24/7 channels).
     * Measured on the TV's data: logo cards are >80% near-white pixels, real art <6%.
     */
    fun isPlaceholder(context: Context, url: String): Boolean = placeholderVerdicts.getOrPut(url) {
        val bytes = fetch(context, sizedUrl(url, SMALL_FETCH_W)) ?: return@getOrPut false
        val tiny = decode(bytes, 32) ?: return@getOrPut false
        val px = IntArray(tiny.width * tiny.height)
        tiny.getPixels(px, 0, tiny.width, 0, 0, tiny.width, tiny.height)
        tiny.recycle()
        val white = px.count { (it shr 16 and 0xFF) > 225 && (it shr 8 and 0xFF) > 225 && (it and 0xFF) > 225 }
        white > px.size / 2
    }

    fun pruneDisk(context: Context) {
        val cutoff = System.currentTimeMillis() - DISK_MAX_AGE_MS
        dir(context).listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }

    private fun key(url: String, widthPx: Int, backdrop: Boolean) = if (backdrop) "b|$url" else "$widthPx|$url"

    /** Asks the CDN for the size we draw instead of the default the app chose. */
    private fun sizedUrl(url: String, widthPx: Int): String {
        val w = if (widthPx > 700) 1280 else 640
        val h = w * 9 / 16
        return when {
            "indazn.com" in url -> url.replace(Regex("width=\\d+&height=\\d+"), "width=$w&height=$h")
            "images-amazon.com" in url -> url.replace(Regex("_UY\\d+_UX\\d+_"), "_UY${h}_UX${w}_")
            else -> url
        }
    }

    private fun fetch(context: Context, url: String): ByteArray? {
        if (url.startsWith("content://") || url.startsWith("android.resource://")) {
            return try {
                context.contentResolver.openInputStream(android.net.Uri.parse(url))?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
        }
        if (!url.startsWith("https://")) return null
        val file = File(dir(context), sha1(url))
        if (file.exists()) return file.readBytes()
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            try {
                if (conn.responseCode != 200) return null
                val bytes = conn.inputStream.use { it.readBytes() }
                val tmp = File(file.path + ".tmp")
                tmp.writeBytes(bytes)
                tmp.renameTo(file)
                bytes
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decode(bytes: ByteArray, widthPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= widthPx) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        if (bitmap.width <= widthPx * 5 / 4) return bitmap
        val h = bitmap.height * widthPx / bitmap.width
        return Bitmap.createScaledBitmap(bitmap, widthPx, h, true).also { if (it !== bitmap) bitmap.recycle() }
    }

    /**
     * Full-screen background: a 96 px copy, blurred, darkened and faded into the page color
     * in software, then stretched by the GPU. One opaque layer, no runtime blur needed (API 28).
     */
    private fun decodeBackdrop(bytes: ByteArray): Bitmap? {
        val small = decode(bytes, BACKDROP_W) ?: return null
        val w = small.width
        val h = small.height
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        small.recycle()
        repeat(3) { boxBlur(px, w, h, 3) }
        for (y in 0 until h) {
            val t = y.toFloat() / (h - 1)
            // Top: art at 38%, a third desaturated so loud posters stay ambient;
            // the lower part fades into the background color #07070B.
            val keep = 0.38f * (1f - smoothStep(0.2f, 0.8f, t))
            for (x in 0 until w) {
                val c = px[y * w + x]
                val r0 = c shr 16 and 0xFF
                val g0 = c shr 8 and 0xFF
                val b0 = c and 0xFF
                val gray = (r0 * 30 + g0 * 59 + b0 * 11) / 100
                val r = ((r0 * 2 + gray) / 3 * keep + 7 * (1 - keep)).toInt()
                val g = ((g0 * 2 + gray) / 3 * keep + 7 * (1 - keep)).toInt()
                val b = ((b0 * 2 + gray) / 3 * keep + 11 * (1 - keep)).toInt()
                px[y * w + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun smoothStep(a: Float, b: Float, t: Float): Float {
        val x = ((t - a) / (b - a)).coerceIn(0f, 1f)
        return x * x * (3 - 2 * x)
    }

    private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int) {
        val tmp = IntArray(px.size)
        for (pass in 0..1) {
            val src = if (pass == 0) px else tmp
            val dst = if (pass == 0) tmp else px
            for (y in 0 until h) for (x in 0 until w) {
                var rs = 0; var gs = 0; var bs = 0; var n = 0
                for (k in -r..r) {
                    val xx = if (pass == 0) (x + k).coerceIn(0, w - 1) else x
                    val yy = if (pass == 0) y else (y + k).coerceIn(0, h - 1)
                    val c = src[yy * w + xx]
                    rs += c shr 16 and 0xFF; gs += c shr 8 and 0xFF; bs += c and 0xFF; n++
                }
                dst[y * w + x] = (0xFF shl 24) or ((rs / n) shl 16) or ((gs / n) shl 8) or (bs / n)
            }
        }
    }

    private fun dir(context: Context) = File(context.cacheDir, "art").apply { mkdirs() }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
