package com.haisnap.spatialdj.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.haisnap.spatialdj.domain.model.Track
import com.haisnap.spatialdj.domain.model.TrackSource
import java.security.MessageDigest

class AndroidTrackRepository(private val context: Context) : TrackRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val demoTracks = FakeTrackRepository().tracks()

    override fun tracks(): List<Track> = demoTracks + savedUris().mapNotNull(::metadataFor)

    override fun importUris(uris: List<String>): List<Track> {
        val merged = (savedUris() + uris).distinct()
        preferences.edit().putStringSet(KEY_URIS, merged.toSet()).apply()
        return demoTracks + merged.mapNotNull(::metadataFor)
    }

    private fun savedUris(): List<String> = preferences.getStringSet(KEY_URIS, emptySet()).orEmpty().sorted()

    private fun metadataFor(uriText: String): Track? = runCatching {
        val uri = Uri.parse(uriText)
        val fallbackTitle = displayName(uri)?.substringBeforeLast('.') ?: "LOCAL TRACK"
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.div(1_000L)?.coerceAtLeast(1L)?.toInt() ?: 0
            Track(
                id = "local-${sha1(uriText).take(12)}",
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf(String::isNotBlank)
                    ?: fallbackTitle,
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf(String::isNotBlank)
                    ?: "LOCAL FILE",
                durationSeconds = duration,
                bpm = 0,
                source = TrackSource.Local(uriText),
            )
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun displayName(uri: Uri): String? = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

    private fun sha1(value: String): String = MessageDigest.getInstance("SHA-1")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val PREFERENCES = "spatial_dj_tracks"
        const val KEY_URIS = "persisted_audio_uris"
    }
}
