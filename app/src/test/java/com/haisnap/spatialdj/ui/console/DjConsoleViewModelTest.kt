package com.haisnap.spatialdj.ui.console

import com.haisnap.spatialdj.data.repository.FakeTrackRepository
import com.haisnap.spatialdj.audio.DeckAudioSnapshot
import com.haisnap.spatialdj.audio.DjAudioEngine
import com.haisnap.spatialdj.domain.model.CrossfadeGains
import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.PlaybackState
import com.haisnap.spatialdj.domain.model.Track
import com.haisnap.spatialdj.domain.usecase.CalculateCrossfadeGainsUseCase
import com.haisnap.spatialdj.domain.usecase.SelectTrackForDeckUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DjConsoleViewModelTest {
    private fun createViewModel(engine: DjAudioEngine = TestAudioEngine()) = DjConsoleViewModel(
        FakeTrackRepository(),
        SelectTrackForDeckUseCase(),
        CalculateCrossfadeGainsUseCase(),
        engine,
    )

    @Test
    fun initLoadsLibrary() {
        assertEquals(4, createViewModel().state.value.tracks.size)
    }

    @Test
    fun firstTrackLoadsDeckA() {
        val viewModel = createViewModel()
        viewModel.onEvent(DjConsoleEvent.LoadTrack("neon-drift"))
        assertEquals("neon-drift", viewModel.state.value.deckA.track?.id)
        assertEquals(DeckId.A, viewModel.state.value.activeDeck)
    }

    @Test
    fun secondTrackLoadsDeckB() {
        val viewModel = createViewModel()
        viewModel.onEvent(DjConsoleEvent.LoadTrack("neon-drift"))
        viewModel.onEvent(DjConsoleEvent.LoadTrack("midnight-circuit"))
        assertEquals("midnight-circuit", viewModel.state.value.deckB.track?.id)
        assertEquals(DeckId.B, viewModel.state.value.activeDeck)
    }

    @Test
    fun playbackOnEmptyDeckIsIgnored() {
        val viewModel = createViewModel()
        viewModel.onEvent(DjConsoleEvent.TogglePlayback(DeckId.A))
        assertNull(viewModel.state.value.deckA.track)
        assertEquals(PlaybackState.Empty, viewModel.state.value.deckA.playbackState)
    }

    @Test
    fun crossfaderClampsOutOfRangeInput() {
        val viewModel = createViewModel()
        viewModel.onEvent(DjConsoleEvent.SetCrossfader(2f))
        assertEquals(1f, viewModel.state.value.crossfader, 0.001f)
        assertEquals(0f, viewModel.state.value.crossfadeGains.deckA, 0.001f)
    }

    @Test
    fun playbackCueAndScratchReachAudioEngine() {
        val engine = TestAudioEngine()
        val viewModel = createViewModel(engine)
        viewModel.onEvent(DjConsoleEvent.LoadTrack("neon-drift"))
        viewModel.onEvent(DjConsoleEvent.TogglePlayback(DeckId.A))
        viewModel.onEvent(DjConsoleEvent.Scratch(DeckId.A, 0.4f))
        viewModel.onEvent(DjConsoleEvent.Cue(DeckId.A))

        assertEquals(DeckId.A, engine.loadedDeck)
        assertEquals(0.4f, engine.lastScratch, 0.001f)
        assertEquals(1, engine.cueCount)
        assertEquals(PlaybackState.Ready, viewModel.state.value.deckA.playbackState)
    }

    @Test
    fun mixerValuesAreSentToAudioEngine() {
        val engine = TestAudioEngine()
        val viewModel = createViewModel(engine)
        viewModel.onEvent(DjConsoleEvent.SetVolume(DeckId.B, 0.42f))
        viewModel.onEvent(DjConsoleEvent.SetBass(DeckId.B, 0.6f))
        viewModel.onEvent(DjConsoleEvent.SetTreble(DeckId.B, -0.3f))
        viewModel.onEvent(DjConsoleEvent.SetTempo(DeckId.B, 1.2f))
        viewModel.onEvent(DjConsoleEvent.SetCrossfader(0.8f))

        assertEquals(0.42f, engine.volume, 0.001f)
        assertEquals(0.6f, engine.bass, 0.001f)
        assertEquals(-0.3f, engine.treble, 0.001f)
        assertEquals(1.2f, engine.tempo, 0.001f)
        assertEquals(0.4f, engine.crossfade.deckA, 0.001f)
    }

    @Test
    fun languageToggleDoesNotResetLoadedDeck() {
        val viewModel = createViewModel()
        viewModel.onEvent(DjConsoleEvent.LoadTrack("neon-drift"))

        viewModel.onEvent(DjConsoleEvent.ToggleLanguage)
        assertEquals(UiLanguage.English, viewModel.state.value.language)
        assertEquals("neon-drift", viewModel.state.value.deckA.track?.id)
        assertEquals("DECK A READY", viewModel.state.value.status.localized(UiLanguage.English))

        viewModel.onEvent(DjConsoleEvent.ToggleLanguage)
        assertEquals(UiLanguage.Chinese, viewModel.state.value.language)
        assertEquals("唱盘 A 已就绪", viewModel.state.value.status.localized(UiLanguage.Chinese))
    }

    private class TestAudioEngine : DjAudioEngine {
        var loadedDeck: DeckId? = null
        var lastScratch = 0f
        var cueCount = 0
        var volume = 0f
        var bass = 0f
        var treble = 0f
        var tempo = 0f
        var crossfade = CrossfadeGains(1f, 1f)
        private var playing = false

        override fun setListener(listener: (DeckAudioSnapshot) -> Unit) = Unit
        override fun load(deckId: DeckId, track: Track, onComplete: (Result<Float>) -> Unit) {
            loadedDeck = deckId
            onComplete(Result.success(track.durationSeconds.toFloat()))
        }
        override fun togglePlayback(deckId: DeckId): Boolean {
            playing = !playing
            return playing
        }
        override fun stop(deckId: DeckId) { playing = false }
        override fun cue(deckId: DeckId) { cueCount++; playing = false }
        override fun scratch(deckId: DeckId, deltaSeconds: Float) { lastScratch = deltaSeconds }
        override fun setVolume(deckId: DeckId, value: Float) { volume = value }
        override fun setTempo(deckId: DeckId, value: Float) { tempo = value }
        override fun setBass(deckId: DeckId, value: Float) { bass = value }
        override fun setTreble(deckId: DeckId, value: Float) { treble = value }
        override fun setCrossfadeGains(gains: CrossfadeGains) { crossfade = gains }
        override fun release() = Unit
    }
}
