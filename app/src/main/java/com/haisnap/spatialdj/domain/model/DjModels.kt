package com.haisnap.spatialdj.domain.model

enum class DeckId { A, B }

enum class PlaybackState { Empty, Ready, Playing, Paused }

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val bpm: Int,
)

data class DeckState(
    val id: DeckId,
    val track: Track? = null,
    val playbackState: PlaybackState = PlaybackState.Empty,
    val progress: Float = 0f,
    val volume: Float = 0.82f,
    val tempo: Float = 1f,
    val bass: Float = 0f,
    val treble: Float = 0f,
)

data class CrossfadeGains(val deckA: Float, val deckB: Float)
