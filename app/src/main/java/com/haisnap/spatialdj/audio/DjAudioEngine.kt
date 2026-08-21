package com.haisnap.spatialdj.audio

import com.haisnap.spatialdj.domain.model.CrossfadeGains
import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.Track

data class DeckAudioSnapshot(
    val deckId: DeckId,
    val positionSeconds: Float,
    val durationSeconds: Float,
    val isPlaying: Boolean,
    val peak: Float,
)

interface DjAudioEngine {
    fun setListener(listener: (DeckAudioSnapshot) -> Unit)
    fun load(deckId: DeckId, track: Track, onComplete: (Result<Float>) -> Unit)
    fun togglePlayback(deckId: DeckId): Boolean
    fun stop(deckId: DeckId)
    fun cue(deckId: DeckId)
    fun scratch(deckId: DeckId, deltaSeconds: Float)
    fun setVolume(deckId: DeckId, value: Float)
    fun setTempo(deckId: DeckId, value: Float)
    fun setBass(deckId: DeckId, value: Float)
    fun setTreble(deckId: DeckId, value: Float)
    fun setCrossfadeGains(gains: CrossfadeGains)
    fun release()
}

/** JVM-safe test engine; the Android factory replaces it with [PcmDjAudioEngine]. */
class NoOpDjAudioEngine : DjAudioEngine {
    private val loaded = mutableSetOf<DeckId>()
    private val playing = mutableSetOf<DeckId>()
    private var listener: (DeckAudioSnapshot) -> Unit = {}

    override fun setListener(listener: (DeckAudioSnapshot) -> Unit) {
        this.listener = listener
    }

    override fun load(deckId: DeckId, track: Track, onComplete: (Result<Float>) -> Unit) {
        loaded += deckId
        playing -= deckId
        onComplete(Result.success(track.durationSeconds.toFloat()))
    }

    override fun togglePlayback(deckId: DeckId): Boolean {
        if (deckId !in loaded) return false
        if (!playing.add(deckId)) playing.remove(deckId)
        return deckId in playing
    }

    override fun stop(deckId: DeckId) {
        playing -= deckId
        listener(DeckAudioSnapshot(deckId, 0f, 0f, false, 0f))
    }

    override fun cue(deckId: DeckId) = stop(deckId)
    override fun scratch(deckId: DeckId, deltaSeconds: Float) = Unit
    override fun setVolume(deckId: DeckId, value: Float) = Unit
    override fun setTempo(deckId: DeckId, value: Float) = Unit
    override fun setBass(deckId: DeckId, value: Float) = Unit
    override fun setTreble(deckId: DeckId, value: Float) = Unit
    override fun setCrossfadeGains(gains: CrossfadeGains) = Unit
    override fun release() = Unit
}
