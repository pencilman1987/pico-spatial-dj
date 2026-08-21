package com.haisnap.spatialdj.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateCrossfadeGainsUseCaseTest {
    private val useCase = CalculateCrossfadeGainsUseCase()

    @Test
    fun leftEdgeMutesDeckB() {
        val gains = useCase(0f)
        assertEquals(1f, gains.deckA, 0.001f)
        assertEquals(0f, gains.deckB, 0.001f)
    }

    @Test
    fun rightEdgeMutesDeckA() {
        val gains = useCase(1f)
        assertEquals(0f, gains.deckA, 0.001f)
        assertEquals(1f, gains.deckB, 0.001f)
    }

    @Test
    fun centerKeepsBothDecksAtFullGain() {
        val gains = useCase(0.5f)
        assertEquals(1f, gains.deckA, 0.001f)
        assertEquals(1f, gains.deckB, 0.001f)
    }
}
