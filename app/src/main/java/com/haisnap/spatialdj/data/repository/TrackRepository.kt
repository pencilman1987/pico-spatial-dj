package com.haisnap.spatialdj.data.repository

import com.haisnap.spatialdj.domain.model.Track

interface TrackRepository {
    fun tracks(): List<Track>
}

class FakeTrackRepository : TrackRepository {
    override fun tracks(): List<Track> =
        listOf(
            Track("neon-drift", "Neon Drift", "Astra", 238, 124),
            Track("midnight-circuit", "Midnight Circuit", "Kite System", 264, 128),
            Track("soft-machines", "Soft Machines", "Lumen", 221, 118),
            Track("afterglow", "Afterglow", "No Signal", 246, 126),
        )
}
