package com.haisnap.spatialdj.data.repository

import com.haisnap.spatialdj.domain.model.Track
import com.haisnap.spatialdj.domain.model.TrackSource

interface TrackRepository {
    fun tracks(): List<Track>
    fun importUris(uris: List<String>): List<Track> = tracks()
}

class FakeTrackRepository : TrackRepository {
    override fun tracks(): List<Track> =
        listOf(
            Track("neon-drift", "Neon Drift", "Astra", 48, 124, TrackSource.Demo(0)),
            Track("midnight-circuit", "Midnight Circuit", "Kite System", 48, 128, TrackSource.Demo(1)),
            Track("soft-machines", "Soft Machines", "Lumen", 48, 118, TrackSource.Demo(2)),
            Track("afterglow", "Afterglow", "No Signal", 48, 126, TrackSource.Demo(3)),
        )
}
