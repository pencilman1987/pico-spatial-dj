package com.haisnap.spatialdj.ui.console

import com.haisnap.spatialdj.domain.model.CrossfadeGains
import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.DeckState
import com.haisnap.spatialdj.domain.model.Track

data class DjConsoleUiState(
    val deckA: DeckState = DeckState(DeckId.A),
    val deckB: DeckState = DeckState(DeckId.B),
    val activeDeck: DeckId = DeckId.A,
    val crossfader: Float = 0.5f,
    val crossfadeGains: CrossfadeGains = CrossfadeGains(1f, 1f),
    val tracks: List<Track> = emptyList(),
    val selectedTrackId: String? = null,
    val language: UiLanguage = UiLanguage.Chinese,
    val status: DjStatus = DjStatus.AudioReady,
)

sealed interface DjConsoleEvent {
    data class SelectDeck(val deckId: DeckId) : DjConsoleEvent
    data class LoadTrack(val trackId: String) : DjConsoleEvent
    data class ImportTracks(val uris: List<String>) : DjConsoleEvent
    data class TogglePlayback(val deckId: DeckId) : DjConsoleEvent
    data class Stop(val deckId: DeckId) : DjConsoleEvent
    data class Cue(val deckId: DeckId) : DjConsoleEvent
    data class Scratch(val deckId: DeckId, val deltaSeconds: Float) : DjConsoleEvent
    data class SetCrossfader(val value: Float) : DjConsoleEvent
    data class SetVolume(val deckId: DeckId, val value: Float) : DjConsoleEvent
    data class SetTempo(val deckId: DeckId, val value: Float) : DjConsoleEvent
    data class SetBass(val deckId: DeckId, val value: Float) : DjConsoleEvent
    data class SetTreble(val deckId: DeckId, val value: Float) : DjConsoleEvent
    data object ToggleLanguage : DjConsoleEvent
}
