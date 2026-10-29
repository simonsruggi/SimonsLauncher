package com.simonslauncher.app.model

/** Apps shown first in the row, in this order; everything else follows alphabetically. */
enum class Brand(val displayName: String, val packages: List<String>) {
    DAZN("DAZN", listOf("com.dazn", "it.dazn.androidtv", "com.dazn.android")),
    NETFLIX("Netflix", listOf("com.netflix.ninja")),
    PRIME_VIDEO("Prime Video", listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod")),
    // Third-party YouTube client for Android TV: takes YouTube's place in the row.
    SMARTTUBE("SmartTube", listOf("org.smarttube.stable", "org.smarttube.beta", "com.teamsmart.videomanager.tv")),
    YOUTUBE("YouTube", listOf("com.google.android.youtube.tv", "com.google.android.youtube")),
    DISNEY("Disney+", listOf("com.disney.disneyplus")),
    RAIPLAY("RaiPlay", listOf("it.rainet.androidtv", "it.rainet.raiplay", "com.rai.raiplay")),
    MEDIASET("Mediaset Infinity", listOf("it.mediaset.infinitytv")),
    NOW("NOW", listOf("com.nowtv.it", "com.sky.nowtv"));

    companion object {
        fun of(packageName: String): Brand? = entries.firstOrNull { packageName in it.packages }
    }
}
