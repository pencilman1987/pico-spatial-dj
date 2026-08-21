package com.haisnap.spatialdj.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoTrackSynthesizerTest {
    @Test
    fun generatedDemoIsStereoAndAudible() {
        val audio = DemoTrackSynthesizer.create(pattern = 1, bpm = 126)

        assertEquals(48_000, audio.sampleRate)
        assertEquals(2, audio.channels)
        assertEquals(48f, audio.durationSeconds, 0.01f)
        assertTrue(DemoTrackSynthesizer.hasAudibleSignal(audio))
    }
}
