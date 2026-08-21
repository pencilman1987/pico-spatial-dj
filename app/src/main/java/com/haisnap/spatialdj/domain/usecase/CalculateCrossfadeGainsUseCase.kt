package com.haisnap.spatialdj.domain.usecase

import com.haisnap.spatialdj.domain.model.CrossfadeGains

class CalculateCrossfadeGainsUseCase {
    operator fun invoke(value: Float): CrossfadeGains {
        val normalized = value.coerceIn(0f, 1f)
        return if (normalized < 0.5f) {
            CrossfadeGains(deckA = 1f, deckB = normalized * 2f)
        } else {
            CrossfadeGains(deckA = (1f - normalized) * 2f, deckB = 1f)
        }
    }
}
