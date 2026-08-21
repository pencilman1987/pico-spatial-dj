package com.haisnap.spatialdj.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

data class PcmAudio(
    val samples: ShortArray,
    val sampleRate: Int,
    val channels: Int,
) {
    val frameCount: Int get() = samples.size / channels
    val durationSeconds: Float get() = frameCount.toFloat() / sampleRate
}

object DemoTrackSynthesizer {
    private const val SAMPLE_RATE = 48_000
    private const val DURATION_SECONDS = 48

    fun create(pattern: Int, bpm: Int): PcmAudio {
        val frames = SAMPLE_RATE * DURATION_SECONDS
        val output = ShortArray(frames * 2)
        val random = Random(7_919 + pattern * 97)
        val beatSeconds = 60.0 / bpm
        val bassNotes = intArrayOf(45, 45, 53, 41, 48, 48, 55, 43)
        var kickPhase = 0.0
        var bassPhase = 0.0
        var hatNoise = 0.0

        repeat(frames) { frame ->
            val time = frame.toDouble() / SAMPLE_RATE
            val beat = time / beatSeconds
            val beatPhase = beat - beat.toInt()
            val halfBeatPhase = (beat * 2.0) - (beat * 2.0).toInt()
            val barBeat = beat.toInt() % 4

            val kickEnvelope = exp(-beatPhase * 18.0)
            val kickFrequency = 48.0 + 75.0 * exp(-beatPhase * 28.0)
            kickPhase += 2.0 * PI * kickFrequency / SAMPLE_RATE
            val kick = sin(kickPhase) * kickEnvelope * 0.72

            val snarePhase = ((beat + 3.0) % 4.0)
            val snareEnvelope = if (snarePhase < 0.22) exp(-snarePhase * 22.0) else 0.0
            val snare = (random.nextDouble() * 2.0 - 1.0) * snareEnvelope * 0.28

            val hatEnvelope = exp(-halfBeatPhase * 55.0)
            hatNoise = hatNoise * 0.35 + (random.nextDouble() * 2.0 - 1.0) * 0.65
            val hat = hatNoise * hatEnvelope * (0.11 + pattern * 0.012)

            val note = bassNotes[(beat.toInt() / 2 + pattern) % bassNotes.size]
            val bassFrequency = 440.0 * Math.pow(2.0, (note - 69) / 12.0)
            bassPhase += 2.0 * PI * bassFrequency / SAMPLE_RATE
            val bassGate = if (beatPhase < 0.72) 1.0 else exp(-(beatPhase - 0.72) * 25.0)
            val bass = (sin(bassPhase) + sin(bassPhase * 2.0) * 0.22) * bassGate * 0.22

            val chordFrequency = 110.0 * (1.0 + pattern * 0.055)
            val pad = (sin(2.0 * PI * chordFrequency * time) +
                sin(2.0 * PI * chordFrequency * 1.5 * time) * 0.5) * 0.055
            val fill = if (barBeat == 3 && beatPhase > 0.72) hat * 1.7 else 0.0
            val mono = (kick + snare + hat + bass + pad + fill).coerceIn(-0.96, 0.96)
            val pan = sin(time * 0.31 + pattern) * 0.08
            output[frame * 2] = (mono * (1.0 - pan) * Short.MAX_VALUE).toInt().toShort()
            output[frame * 2 + 1] = (mono * (1.0 + pan) * Short.MAX_VALUE).toInt().toShort()
        }
        return PcmAudio(output, SAMPLE_RATE, 2)
    }

    fun hasAudibleSignal(audio: PcmAudio): Boolean =
        audio.samples.maxOfOrNull { max(it.toInt(), -it.toInt()) }?.let { it > 1_000 } == true
}
