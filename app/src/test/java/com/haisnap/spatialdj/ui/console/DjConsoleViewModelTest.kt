package com.haisnap.spatialdj.ui.console

import com.haisnap.spatialdj.data.repository.FakeTrackRepository
import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.PlaybackState
import com.haisnap.spatialdj.domain.usecase.CalculateCrossfadeGainsUseCase
import com.haisnap.spatialdj.domain.usecase.SelectTrackForDeckUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DjConsoleViewModelTest {
    private fun createViewModel() = DjConsoleViewModel(
        FakeTrackRepository(),
        SelectTrackForDeckUseCase(),
        CalculateCrossfadeGainsUseCase(),
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
}
