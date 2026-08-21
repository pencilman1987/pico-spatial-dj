package com.haisnap.spatialdj.ui.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.haisnap.spatialdj.data.repository.FakeTrackRepository
import com.haisnap.spatialdj.data.repository.TrackRepository
import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.DeckState
import com.haisnap.spatialdj.domain.model.PlaybackState
import com.haisnap.spatialdj.domain.usecase.CalculateCrossfadeGainsUseCase
import com.haisnap.spatialdj.domain.usecase.SelectTrackForDeckUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DjConsoleViewModel(
    private val trackRepository: TrackRepository,
    private val selectTrackForDeck: SelectTrackForDeckUseCase,
    private val calculateCrossfadeGains: CalculateCrossfadeGainsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(DjConsoleUiState(tracks = trackRepository.tracks()))
    val state: StateFlow<DjConsoleUiState> = _state.asStateFlow()

    fun onEvent(event: DjConsoleEvent) {
        when (event) {
            is DjConsoleEvent.SelectDeck -> _state.update { it.copy(activeDeck = event.deckId) }
            is DjConsoleEvent.LoadTrack -> loadTrack(event.trackId)
            is DjConsoleEvent.TogglePlayback -> updateDeck(event.deckId) { deck ->
                if (deck.track == null) deck else deck.copy(
                    playbackState = if (deck.playbackState == PlaybackState.Playing) PlaybackState.Paused else PlaybackState.Playing,
                )
            }
            is DjConsoleEvent.Stop -> updateDeck(event.deckId) { deck ->
                deck.copy(
                    playbackState = if (deck.track == null) PlaybackState.Empty else PlaybackState.Ready,
                    progress = 0f,
                )
            }
            is DjConsoleEvent.SetCrossfader -> setCrossfader(event.value)
            is DjConsoleEvent.SetVolume -> updateDeck(event.deckId) { it.copy(volume = event.value.coerceIn(0f, 1f)) }
            is DjConsoleEvent.SetTempo -> updateDeck(event.deckId) { it.copy(tempo = event.value.coerceIn(0.5f, 1.5f)) }
            is DjConsoleEvent.SetBass -> updateDeck(event.deckId) { it.copy(bass = event.value.coerceIn(-1f, 1f)) }
            is DjConsoleEvent.SetTreble -> updateDeck(event.deckId) { it.copy(treble = event.value.coerceIn(-1f, 1f)) }
        }
    }

    private fun loadTrack(trackId: String) {
        _state.update { current ->
            val track = current.tracks.firstOrNull { it.id == trackId } ?: return@update current
            val target = selectTrackForDeck(current.activeDeck, current.deckA.track, current.deckB.track)
            val loaded = deck(current, target).copy(track = track, playbackState = PlaybackState.Ready, progress = 0f)
            current.withDeck(target, loaded).copy(activeDeck = target, selectedTrackId = track.id)
        }
    }

    private fun setCrossfader(value: Float) {
        val normalized = value.coerceIn(0f, 1f)
        _state.update { it.copy(crossfader = normalized, crossfadeGains = calculateCrossfadeGains(normalized)) }
    }

    private fun updateDeck(deckId: DeckId, transform: (DeckState) -> DeckState) {
        _state.update { current -> current.withDeck(deckId, transform(deck(current, deckId))) }
    }

    private fun deck(state: DjConsoleUiState, deckId: DeckId): DeckState =
        if (deckId == DeckId.A) state.deckA else state.deckB

    private fun DjConsoleUiState.withDeck(deckId: DeckId, deck: DeckState): DjConsoleUiState =
        if (deckId == DeckId.A) copy(deckA = deck) else copy(deckB = deck)

    class Factory(
        private val repository: TrackRepository = FakeTrackRepository(),
        private val selectTrackForDeck: SelectTrackForDeckUseCase = SelectTrackForDeckUseCase(),
        private val calculateCrossfadeGains: CalculateCrossfadeGainsUseCase = CalculateCrossfadeGainsUseCase(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DjConsoleViewModel(repository, selectTrackForDeck, calculateCrossfadeGains) as T
    }
}
