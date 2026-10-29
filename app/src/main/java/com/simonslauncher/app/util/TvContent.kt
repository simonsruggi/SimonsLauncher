package com.simonslauncher.app.util

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import com.simonslauncher.app.model.Brand
import com.simonslauncher.app.model.Program

/**
 * Reads the preview channels that the installed apps publish (Netflix, DAZN, Prime Video...).
 * Needs READ_TV_LISTINGS: without it TvProvider only returns the launcher's own rows.
 */
object TvContent {

    private val PROJECTION = arrayOf(
        TvContract.PreviewPrograms._ID,
        TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME,
        TvContract.PreviewPrograms.COLUMN_TITLE,
        TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION,
        TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
        TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI,
        TvContract.PreviewPrograms.COLUMN_LIVE,
        TvContract.PreviewPrograms.COLUMN_INTENT_URI
    )

    /** Programs per package, in the order each app published them, duplicates removed. */
    fun load(context: Context): Map<String, List<Program>> {
        val programs = mutableListOf<Program>()
        try {
            context.contentResolver.query(TvContract.PreviewPrograms.CONTENT_URI, PROJECTION, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val pkg = c.getString(1) ?: continue
                    // Only apps that have a tile in the launcher: their titles open through it.
                    if (Brand.of(pkg) == null) continue
                    val title = c.getString(2)?.trim().orEmpty()
                    if (title.isEmpty()) continue
                    programs.add(
                        Program(
                            id = c.getLong(0),
                            packageName = pkg,
                            title = title,
                            description = c.getString(3)?.trim()?.takeIf { it.isNotEmpty() },
                            artUrl = (c.getString(4) ?: c.getString(5))?.let(::secureUrl),
                            live = c.getInt(6) == 1,
                            intentUri = c.getString(7)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            return emptyMap()
        }
        return programs
            .groupBy { it.packageName }
            .mapValues { (_, list) -> list.distinctBy { it.title } }
    }

    /** Android 9 blocks cleartext HTTP; the image CDNs used by the apps all serve HTTPS too. */
    private fun secureUrl(url: String): String =
        if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url

    /**
     * Deep link into the title. The intent comes from another app's data, so it is pinned to
     * that app's package and stripped of selectors and URI-grant flags.
     */
    fun intentFor(program: Program): Intent? {
        val uri = program.intentUri ?: return null
        return try {
            val intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
            val component = intent.component
            if (component != null && component.packageName != program.packageName) return null
            intent.selector = null
            intent.setPackage(program.packageName)
            intent.flags = (intent.flags and GRANT_FLAGS.inv()) or Intent.FLAG_ACTIVITY_NEW_TASK
            intent
        } catch (_: Exception) {
            null
        }
    }

    private const val GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
}
