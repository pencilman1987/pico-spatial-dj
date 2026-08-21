package com.haisnap.spatialdj.domain.usecase

import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.Track

class SelectTrackForDeckUseCase {
    operator fun invoke(requestedDeck: DeckId, deckATrack: Track?, deckBTrack: Track?): DeckId =
        when {
            deckATrack == null -> DeckId.A
            deckBTrack == null -> DeckId.B
            else -> requestedDeck
        }
}
