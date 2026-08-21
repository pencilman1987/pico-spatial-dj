package com.haisnap.spatialdj.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder

class AndroidAudioDecoder(private val context: Context) {
    fun decode(uriText: String): PcmAudio {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, Uri.parse(uriText), null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("文件中没有可解码的音轨")
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: error("音频格式未知")
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            val output = ShortAccumulator()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var sampleRate = inputFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, 48_000)
            var channels = inputFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, 2)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            val maximumSamples = sampleRate * channels * MAX_DURATION_SECONDS

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex) ?: error("无法取得解码输入缓冲区")
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = codec.outputFormat
                        sampleRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        channels = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                        pcmEncoding = format.intOr(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        codec.getOutputBuffer(outputIndex)?.let { buffer ->
                            if (info.size > 0) {
                                val slice = buffer.duplicate().order(ByteOrder.nativeOrder()).apply {
                                    position(info.offset)
                                    limit(info.offset + info.size)
                                }.slice().order(ByteOrder.nativeOrder())
                                when (pcmEncoding) {
                                    AudioFormat.ENCODING_PCM_FLOAT -> {
                                        val floats = slice.asFloatBuffer()
                                        while (floats.hasRemaining() && output.size < maximumSamples) {
                                            output.add((floats.get().coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
                                        }
                                    }
                                    else -> {
                                        val shorts = slice.asShortBuffer()
                                        while (shorts.hasRemaining() && output.size < maximumSamples) output.add(shorts.get())
                                    }
                                }
                            }
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || output.size >= maximumSamples
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            check(output.size > channels) { "音频解码结果为空" }
            return PcmAudio(output.toArray(), sampleRate, channels)
        } finally {
            runCatching { codec?.stop() }
            codec?.release()
            extractor.release()
        }
    }

    private fun MediaFormat.intOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private class ShortAccumulator(initialCapacity: Int = 48_000 * 20) {
        private var data = ShortArray(initialCapacity)
        var size: Int = 0
            private set

        fun add(value: Short) {
            if (size == data.size) data = data.copyOf(data.size * 2)
            data[size++] = value
        }

        fun toArray(): ShortArray = data.copyOf(size)
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
        const val MAX_DURATION_SECONDS = 8 * 60
    }
}
