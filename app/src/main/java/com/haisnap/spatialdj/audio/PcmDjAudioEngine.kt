package com.haisnap.spatialdj.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.haisnap.spatialdj.domain.model.CrossfadeGains
import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.Track
import com.haisnap.spatialdj.domain.model.TrackSource
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

class PcmDjAudioEngine(context: Context) : DjAudioEngine {
    private val lock = Any()
    private val decoder = AndroidAudioDecoder(context.applicationContext)
    private val loader = Executors.newSingleThreadExecutor()
    private val decks = mutableMapOf(DeckId.A to AudioDeck(), DeckId.B to AudioDeck())
    private var gains = CrossfadeGains(1f, 1f)
    private var listener: (DeckAudioSnapshot) -> Unit = {}
    @Volatile private var running = true

    private val minBufferBytes = AudioTrack.getMinBufferSize(
        OUTPUT_SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(MIX_FRAMES * 4)
    private val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(OUTPUT_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build(),
        )
        .setBufferSizeInBytes(minBufferBytes * 2)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        .build()
    private val mixerThread = thread(name = "SpatialDJ-PCM-Mixer", start = true, isDaemon = true) { mixLoop() }

    override fun setListener(listener: (DeckAudioSnapshot) -> Unit) {
        synchronized(lock) { this.listener = listener }
    }

    override fun load(deckId: DeckId, track: Track, onComplete: (Result<Float>) -> Unit) {
        synchronized(lock) {
            decks.getValue(deckId).apply {
                isPlaying = false
                audio = null
                positionFrames = 0.0
                scratchFramesRemaining = 0
                resetFilters()
            }
        }
        loader.execute {
            val result = runCatching {
                when (val source = track.source) {
                    is TrackSource.Demo -> DemoTrackSynthesizer.create(source.pattern, track.bpm)
                    is TrackSource.Local -> decoder.decode(source.uri)
                }
            }
            result.onSuccess { audio ->
                synchronized(lock) {
                    decks.getValue(deckId).apply {
                        this.audio = audio
                        positionFrames = 0.0
                        resetFilters()
                    }
                }
                Log.i(TAG, "Loaded deck $deckId: ${track.title}, ${audio.durationSeconds}s")
            }.onFailure { Log.e(TAG, "Could not load ${track.title}", it) }
            onComplete(result.map { it.durationSeconds })
        }
    }

    override fun togglePlayback(deckId: DeckId): Boolean = synchronized(lock) {
        val deck = decks.getValue(deckId)
        if (deck.audio == null) return@synchronized false
        if (deck.positionFrames >= deck.audio!!.frameCount - 1) deck.positionFrames = 0.0
        deck.isPlaying = !deck.isPlaying
        Log.i(TAG, "Deck $deckId ${if (deck.isPlaying) "PLAY" else "PAUSE"}")
        deck.isPlaying
    }

    override fun stop(deckId: DeckId) {
        synchronized(lock) {
            decks.getValue(deckId).apply {
                isPlaying = false
                positionFrames = 0.0
                scratchFramesRemaining = 0
                peak = 0f
                resetFilters()
            }
        }
    }

    override fun cue(deckId: DeckId) {
        stop(deckId)
    }

    override fun scratch(deckId: DeckId, deltaSeconds: Float) = synchronized(lock) {
        val deck = decks.getValue(deckId)
        val audio = deck.audio ?: return@synchronized
        deck.scratchStep = (deltaSeconds * audio.sampleRate / MIX_FRAMES)
            .coerceIn(-6f, 6f).toDouble()
        deck.scratchFramesRemaining = MIX_FRAMES * 2
        deck.resetFilters()
    }

    override fun setVolume(deckId: DeckId, value: Float) = synchronized(lock) {
        decks.getValue(deckId).volume = value.coerceIn(0f, 1f)
    }

    override fun setTempo(deckId: DeckId, value: Float) = synchronized(lock) {
        decks.getValue(deckId).tempo = value.coerceIn(0.5f, 1.5f)
    }

    override fun setBass(deckId: DeckId, value: Float) = synchronized(lock) {
        decks.getValue(deckId).bass = value.coerceIn(-1f, 1f)
    }

    override fun setTreble(deckId: DeckId, value: Float) = synchronized(lock) {
        decks.getValue(deckId).treble = value.coerceIn(-1f, 1f)
    }

    override fun setCrossfadeGains(gains: CrossfadeGains) = synchronized(lock) {
        this.gains = gains
    }

    override fun release() {
        running = false
        loader.shutdownNow()
        mixerThread.interrupt()
        runCatching { mixerThread.join(500) }
        runCatching { audioTrack.pause() }
        runCatching { audioTrack.flush() }
        audioTrack.release()
    }

    private fun mixLoop() {
        val mixed = ShortArray(MIX_FRAMES * 2)
        runCatching {
            audioTrack.play()
            Log.i(TAG, "Low-latency AudioTrack started: ${audioTrack.audioSessionId}")
            var updateCounter = 0
            while (running) {
                synchronized(lock) {
                    mixed.fill(0)
                    mixDeck(decks.getValue(DeckId.A), gains.deckA, mixed)
                    mixDeck(decks.getValue(DeckId.B), gains.deckB, mixed)
                    if (++updateCounter >= SNAPSHOT_EVERY_BLOCKS) {
                        updateCounter = 0
                        publishSnapshot(DeckId.A, decks.getValue(DeckId.A))
                        publishSnapshot(DeckId.B, decks.getValue(DeckId.B))
                    }
                }
                audioTrack.write(mixed, 0, mixed.size, AudioTrack.WRITE_BLOCKING)
            }
        }.onFailure { if (running) Log.e(TAG, "Mixer stopped unexpectedly", it) }
    }

    private fun mixDeck(deck: AudioDeck, crossfadeGain: Float, mixed: ShortArray) {
        val audio = deck.audio ?: return
        if (!deck.isPlaying && deck.scratchFramesRemaining <= 0) {
            deck.peak *= 0.78f
            return
        }
        val frameLimit = audio.frameCount - 1
        val normalStep = audio.sampleRate.toDouble() / OUTPUT_SAMPLE_RATE * deck.tempo
        val lowGain = dbToLinear(deck.bass * 12f)
        val highGain = dbToLinear(deck.treble * 12f)
        val channelGain = deck.volume * crossfadeGain * MASTER_GAIN
        val lowAlpha = 1.0 - exp(-2.0 * PI * LOW_CUTOFF_HZ / OUTPUT_SAMPLE_RATE)
        var peak = 0f

        for (frame in 0 until MIX_FRAMES) {
            val scratching = deck.scratchFramesRemaining > 0
            if (deck.positionFrames < 0.0 || deck.positionFrames >= frameLimit) {
                deck.positionFrames = deck.positionFrames.coerceIn(0.0, frameLimit.toDouble())
                deck.scratchFramesRemaining = 0
                if (!scratching || deck.positionFrames >= frameLimit) deck.isPlaying = false
                break
            }
            val base = deck.positionFrames.toInt()
            val fraction = (deck.positionFrames - base).toFloat()
            val left = interpolatedSample(audio, base, fraction, 0)
            val right = interpolatedSample(audio, base, fraction, if (audio.channels > 1) 1 else 0)
            val filteredLeft = applyEq(left, lowGain, highGain, lowAlpha, deck, true) * channelGain
            val filteredRight = applyEq(right, lowGain, highGain, lowAlpha, deck, false) * channelGain
            peak = max(peak, max(abs(filteredLeft), abs(filteredRight)))
            val index = frame * 2
            mixed[index] = saturatingAdd(mixed[index], filteredLeft)
            mixed[index + 1] = saturatingAdd(mixed[index + 1], filteredRight)
            deck.positionFrames += if (scratching) deck.scratchStep else normalStep
            if (scratching) deck.scratchFramesRemaining--
        }
        deck.peak = max(peak, deck.peak * 0.76f)
    }

    private fun interpolatedSample(audio: PcmAudio, frame: Int, fraction: Float, channel: Int): Float {
        val current = audio.samples[frame * audio.channels + channel].toFloat() / Short.MAX_VALUE
        val next = audio.samples[(frame + 1) * audio.channels + channel].toFloat() / Short.MAX_VALUE
        return current + (next - current) * fraction
    }

    private fun applyEq(
        sample: Float,
        lowGain: Float,
        highGain: Float,
        alpha: Double,
        deck: AudioDeck,
        left: Boolean,
    ): Float {
        val previous = if (left) deck.lowLeft else deck.lowRight
        val low = (previous + alpha * (sample - previous)).toFloat()
        if (left) deck.lowLeft = low else deck.lowRight = low
        val high = sample - low
        return low * lowGain + high * highGain
    }

    private fun publishSnapshot(deckId: DeckId, deck: AudioDeck) {
        val audio = deck.audio ?: return
        listener(
            DeckAudioSnapshot(
                deckId = deckId,
                positionSeconds = (deck.positionFrames / audio.sampleRate).toFloat(),
                durationSeconds = audio.durationSeconds,
                isPlaying = deck.isPlaying,
                peak = deck.peak.coerceIn(0f, 1f),
            ),
        )
    }

    private fun saturatingAdd(current: Short, sample: Float): Short {
        val value = current.toInt() + (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt()
        return value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    private fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)

    private class AudioDeck {
        var audio: PcmAudio? = null
        var positionFrames = 0.0
        var isPlaying = false
        var volume = 0.82f
        var tempo = 1f
        var bass = 0f
        var treble = 0f
        var lowLeft = 0f
        var lowRight = 0f
        var peak = 0f
        var scratchFramesRemaining = 0
        var scratchStep = 0.0

        fun resetFilters() {
            lowLeft = 0f
            lowRight = 0f
        }
    }

    private companion object {
        const val TAG = "SpatialDJ-Audio"
        const val OUTPUT_SAMPLE_RATE = 48_000
        const val MIX_FRAMES = 256
        const val SNAPSHOT_EVERY_BLOCKS = 8
        const val LOW_CUTOFF_HZ = 280.0
        const val MASTER_GAIN = 0.74f
    }
}
