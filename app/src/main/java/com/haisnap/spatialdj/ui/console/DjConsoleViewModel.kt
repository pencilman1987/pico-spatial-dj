package com.haisnap.spatialdj.ui.console

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.haisnap.spatialdj.audio.DeckAudioSnapshot
import com.haisnap.spatialdj.audio.DjAudioEngine
import com.haisnap.spatialdj.audio.NoOpDjAudioEngine
import com.haisnap.spatialdj.audio.PcmDjAudioEngine
import com.haisnap.spatialdj.data.repository.AndroidTrackRepository
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
    private val audioEngine: DjAudioEngine = NoOpDjAudioEngine(),
) : ViewModel() {
    private val _state = MutableStateFlow(DjConsoleUiState(tracks = trackRepository.tracks()))
    val state: StateFlow<DjConsoleUiState> = _state.asStateFlow()

    init {
        audioEngine.setListener(::onAudioSnapshot)
        audioEngine.setCrossfadeGains(_state.value.crossfadeGains)
    }

    fun onEvent(event: DjConsoleEvent) {
        when (event) {
            is DjConsoleEvent.SelectDeck -> _state.update { it.copy(activeDeck = event.deckId) }
            is DjConsoleEvent.LoadTrack -> loadTrack(event.trackId)
            is DjConsoleEvent.ImportTracks -> importTracks(event.uris)
            is DjConsoleEvent.TogglePlayback -> togglePlayback(event.deckId)
            is DjConsoleEvent.Stop -> {
                audioEngine.stop(event.deckId)
                resetDeck(event.deckId)
            }
            is DjConsoleEvent.Cue -> {
                audioEngine.cue(event.deckId)
                resetDeck(event.deckId)
                _state.update { it.copy(activeDeck = event.deckId) }
            }
            is DjConsoleEvent.Scratch -> {
                audioEngine.scratch(event.deckId, event.deltaSeconds.coerceIn(-2f, 2f))
                _state.update { it.copy(activeDeck = event.deckId) }
            }
            is DjConsoleEvent.SetCrossfader -> setCrossfader(event.value)
            is DjConsoleEvent.SetVolume -> {
                val value = event.value.coerceIn(0f, 1f)
                audioEngine.setVolume(event.deckId, value)
                updateDeck(event.deckId) { it.copy(volume = value) }
            }
            is DjConsoleEvent.SetTempo -> {
                val value = event.value.coerceIn(0.5f, 1.5f)
                audioEngine.setTempo(event.deckId, value)
                updateDeck(event.deckId) { it.copy(tempo = value) }
            }
            is DjConsoleEvent.SetBass -> {
                val value = event.value.coerceIn(-1f, 1f)
                audioEngine.setBass(event.deckId, value)
                updateDeck(event.deckId) { it.copy(bass = value) }
            }
            is DjConsoleEvent.SetTreble -> {
                val value = event.value.coerceIn(-1f, 1f)
                audioEngine.setTreble(event.deckId, value)
                updateDeck(event.deckId) { it.copy(treble = value) }
            }
            DjConsoleEvent.ToggleLanguage -> _state.update { it.copy(language = it.language.toggled()) }
        }
    }

    private fun resetDeck(deckId: DeckId) {
        updateDeck(deckId) { deck ->
            deck.copy(
                playbackState = if (deck.track == null) PlaybackState.Empty else PlaybackState.Ready,
                progress = 0f,
                positionSeconds = 0f,
                level = 0f,
            )
        }
    }

    private fun loadTrack(trackId: String) {
        val current = _state.value
        val track = current.tracks.firstOrNull { it.id == trackId } ?: return
        val target = selectTrackForDeck(current.activeDeck, current.deckA.track, current.deckB.track)
        val loaded = deck(current, target).copy(
            track = track,
            playbackState = PlaybackState.Loading,
            progress = 0f,
            positionSeconds = 0f,
            level = 0f,
        )
        _state.update {
            it.withDeck(target, loaded).copy(
                activeDeck = target,
                selectedTrackId = track.id,
                status = DjStatus.Loading(track.title),
            )
        }
        audioEngine.load(target, track) { result ->
            _state.update { latest ->
                val latestDeck = deck(latest, target)
                if (latestDeck.track?.id != track.id) return@update latest
                result.fold(
                    onSuccess = { duration ->
                        val correctedTrack = track.copy(durationSeconds = duration.toInt().coerceAtLeast(1))
                        latest.withDeck(
                            target,
                            latestDeck.copy(track = correctedTrack, playbackState = PlaybackState.Ready),
                        ).copy(status = DjStatus.DeckReady(target))
                    },
                    onFailure = { error ->
                        latest.withDeck(target, latestDeck.copy(playbackState = PlaybackState.Error))
                            .copy(status = DjStatus.LoadError(error.message ?: "UNSUPPORTED AUDIO"))
                    },
                )
            }
        }
    }

    private fun importTracks(uris: List<String>) {
        if (uris.isEmpty()) return
        val tracks = trackRepository.importUris(uris)
        _state.update { it.copy(tracks = tracks, status = DjStatus.Imported(uris.size)) }
    }

    private fun togglePlayback(deckId: DeckId) {
        val currentDeck = deck(_state.value, deckId)
        if (currentDeck.track == null || currentDeck.playbackState == PlaybackState.Loading || currentDeck.playbackState == PlaybackState.Error) return
        val isPlaying = audioEngine.togglePlayback(deckId)
        updateDeck(deckId) { it.copy(playbackState = if (isPlaying) PlaybackState.Playing else PlaybackState.Paused) }
    }

    private fun onAudioSnapshot(snapshot: DeckAudioSnapshot) {
        updateDeck(snapshot.deckId) { deck ->
            val duration = snapshot.durationSeconds.coerceAtLeast(0.001f)
            val ended = !snapshot.isPlaying && snapshot.positionSeconds >= duration - 0.05f
            deck.copy(
                positionSeconds = snapshot.positionSeconds.coerceIn(0f, duration),
                progress = (snapshot.positionSeconds / duration).coerceIn(0f, 1f),
                level = snapshot.peak,
                playbackState = when {
                    snapshot.isPlaying -> PlaybackState.Playing
                    ended -> PlaybackState.Ready
                    deck.playbackState == PlaybackState.Playing -> PlaybackState.Paused
                    else -> deck.playbackState
                },
            )
        }
    }

    private fun setCrossfader(value: Float) {
        val normalized = value.coerceIn(0f, 1f)
        val gains = calculateCrossfadeGains(normalized)
        audioEngine.setCrossfadeGains(gains)
        _state.update { it.copy(crossfader = normalized, crossfadeGains = gains) }
    }

    private fun updateDeck(deckId: DeckId, transform: (DeckState) -> DeckState) {
        _state.update { current -> current.withDeck(deckId, transform(deck(current, deckId))) }
    }

    private fun deck(state: DjConsoleUiState, deckId: DeckId): DeckState =
        if (deckId == DeckId.A) state.deckA else state.deckB

    private fun DjConsoleUiState.withDeck(deckId: DeckId, deck: DeckState): DjConsoleUiState =
        if (deckId == DeckId.A) copy(deckA = deck) else copy(deckB = deck)

    override fun onCleared() {
        audioEngine.release()
        super.onCleared()
    }

    class Factory(
        context: Context? = null,
        private val repository: TrackRepository = context?.let { AndroidTrackRepository(it.applicationContext) }
            ?: FakeTrackRepository(),
        private val selectTrackForDeck: SelectTrackForDeckUseCase = SelectTrackForDeckUseCase(),
        private val calculateCrossfadeGains: CalculateCrossfadeGainsUseCase = CalculateCrossfadeGainsUseCase(),
        private val audioEngine: DjAudioEngine = context?.let { PcmDjAudioEngine(it.applicationContext) }
            ?: NoOpDjAudioEngine(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DjConsoleViewModel(repository, selectTrackForDeck, calculateCrossfadeGains, audioEngine) as T
    }
}
