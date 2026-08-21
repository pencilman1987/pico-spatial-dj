package com.haisnap.spatialdj.domain.usecase

import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectTrackForDeckUseCaseTest {
    private val useCase = SelectTrackForDeckUseCase()
    private val track = Track("id", "Track", "Artist", 120, 124)

    @Test
    fun emptyDeckAHasPriority() {
        assertEquals(DeckId.A, useCase(DeckId.B, null, track))
    }

    @Test
    fun emptyDeckBIsUsedAfterDeckA() {
        assertEquals(DeckId.B, useCase(DeckId.A, track, null))
    }

    @Test
    fun requestedDeckIsUsedWhenBothAreLoaded() {
        assertEquals(DeckId.B, useCase(DeckId.B, track, track))
    }
}
