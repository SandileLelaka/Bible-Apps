package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AmbientSoundGenerator {

    companion object {
        private const val TAG = "AmbientSoundGenerator"
        private const val SAMPLE_RATE = 22050
        private const val DURATION_SECONDS = 120 // 2 minutes focus
    }

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val synthScope = CoroutineScope(Dispatchers.Default)

    var isPlaying = false
        private set

    var currentSeconds = 0
        private set

    private var onProgressCallback: ((Int) -> Unit)? = null
    private var onCompletionCallback: (() -> Unit)? = null

    fun setOnProgressCallback(callback: (Int) -> Unit) {
        onProgressCallback = callback
    }

    fun setOnCompletionCallback(callback: () -> Unit) {
        onCompletionCallback = callback
    }

    fun start(styleIndex: Int) {
        if (isPlaying) {
            stop()
        }

        isPlaying = true
        currentSeconds = 0
        onProgressCallback?.invoke(0)

        synthJob = synthScope.launch {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufferSize.coerceAtLeast(4096)

            try {
                val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack = track
                track.play()

                val buffer = ShortArray(2048)
                var globalSampleIndex = 0L
                val totalSamples = SAMPLE_RATE * DURATION_SECONDS

                // Define soundscapes: list of voices. Each voice is Pair(Frequency, LFO_Frequency)
                // We will interpolate chords based on progress, or use gorgeous complex drifting pads.
                while (isActive && globalSampleIndex < totalSamples && isPlaying) {
                    val progressRatio = globalSampleIndex.toDouble() / totalSamples
                    val currentProgressSec = (globalSampleIndex / SAMPLE_RATE).toInt()

                    if (currentProgressSec != currentSeconds) {
                        currentSeconds = currentProgressSec
                        launch(Dispatchers.Main) {
                            onProgressCallback?.invoke(currentSeconds)
                        }
                    }

                    // Gentle volume fade-in and fade-out near the boundaries
                    val fadeInFactor = (globalSampleIndex.toDouble() / (SAMPLE_RATE * 3.0)).coerceAtMost(1.0)
                    val fadeOutSamplesLeft = totalSamples - globalSampleIndex
                    val fadeOutFactor = (fadeOutSamplesLeft.toDouble() / (SAMPLE_RATE * 5.0)).coerceAtMost(1.0)
                    val edgeVolumeMultiplier = fadeInFactor * fadeOutFactor

                    for (i in buffer.indices) {
                        val sampleIdx = globalSampleIndex + i
                        val t = sampleIdx.toDouble() / SAMPLE_RATE

                        // Synthesize based on style
                        val value = when (styleIndex) {
                            1 -> {
                                // Celestial Twilight: 6 independent drifting oscillators in Pentatonic Major
                                val v1 = sin(2 * Math.PI * 110.0 * t) * (0.5 + 0.5 * sin(2 * Math.PI * 0.05 * t))
                                val v2 = sin(2 * Math.PI * 165.0 * t) * (0.5 + 0.5 * sin(2 * Math.PI * 0.07 * t))
                                val v3 = sin(2 * Math.PI * 220.0 * t) * (0.5 + 0.5 * sin(2 * Math.PI * 0.11 * t))
                                val v4 = sin(2 * Math.PI * 293.66 * t) * (0.4 + 0.4 * sin(2 * Math.PI * 0.15 * t))
                                val v5 = sin(2 * Math.PI * 330.0 * t) * (0.3 + 0.3 * sin(2 * Math.PI * 0.08 * t))
                                val v6 = sin(2 * Math.PI * 440.0 * t) * (0.2 + 0.2 * sin(2 * Math.PI * 0.13 * t))
                                (v1 + v2 + v3 + v4 + v5 + v6) / 3.5
                            }
                            2 -> {
                                // Soothing Morning: Beautiful major chords swelling
                                // We shift chord every 30 seconds
                                val chordPhase = (t / 30.0).toInt() % 4
                                val freqs = when (chordPhase) {
                                    0 -> listOf(196.0, 246.94, 293.66, 392.0)  // G3, B3, D4, G4
                                    1 -> listOf(261.63, 329.63, 392.0, 523.25) // C4, E4, G4, C5
                                    2 -> listOf(293.66, 369.99, 440.0, 587.33) // D4, F#4, A4, D5
                                    else -> listOf(329.63, 392.0, 493.88, 659.25) // E4, G4, B4, E5
                                }
                                val v1 = sin(2 * Math.PI * freqs[0] * t) * (0.6 + 0.4 * sin(2 * Math.PI * 0.1 * t))
                                val v2 = sin(2 * Math.PI * freqs[1] * t) * (0.5 + 0.4 * sin(2 * Math.PI * 0.08 * t))
                                val v3 = sin(2 * Math.PI * freqs[2] * t) * (0.4 + 0.3 * sin(2 * Math.PI * 0.12 * t))
                                val v4 = sin(2 * Math.PI * freqs[3] * t) * (0.3 + 0.3 * sin(2 * Math.PI * 0.07 * t))
                                (v1 + v2 + v3 + v4) / 2.8
                            }
                            else -> {
                                // Deep Peace: Slow comforting Minor/Major transitions
                                val chordPhase = (t / 30.0).toInt() % 4
                                val freqs = when (chordPhase) {
                                    0 -> listOf(130.81, 196.0, 261.63, 329.63) // C3, G3, C4, E4
                                    1 -> listOf(174.61, 261.63, 349.23, 440.0) // F3, C4, F4, A4
                                    2 -> listOf(196.0, 293.66, 392.0, 493.88)  // G3, D4, G4, B4
                                    else -> listOf(110.0, 220.0, 329.63, 440.0)   // A2, A3, E4, A4
                                }
                                val v1 = sin(2 * Math.PI * freqs[0] * t) * (0.7 + 0.3 * sin(2 * Math.PI * 0.06 * t))
                                val v2 = sin(2 * Math.PI * freqs[1] * t) * (0.5 + 0.4 * sin(2 * Math.PI * 0.09 * t))
                                val v3 = sin(2 * Math.PI * freqs[2] * t) * (0.4 + 0.4 * sin(2 * Math.PI * 0.11 * t))
                                val v4 = sin(2 * Math.PI * freqs[3] * t) * (0.3 + 0.3 * sin(2 * Math.PI * 0.07 * t))
                                (v1 + v2 + v3 + v4) / 2.5
                            }
                        }

                        // Apply soft clipping and scale to 16-bit PCM short values
                        val clipped = value.coerceIn(-1.0, 1.0)
                        val scaled = (clipped * 32767.0 * edgeVolumeMultiplier).toInt()
                        buffer[i] = scaled.toShort()
                    }

                    track.write(buffer, 0, buffer.size)
                    globalSampleIndex += buffer.size
                }

                if (globalSampleIndex >= totalSamples) {
                    currentSeconds = DURATION_SECONDS
                    launch(Dispatchers.Main) {
                        onProgressCallback?.invoke(DURATION_SECONDS)
                        stop()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error playing synthesized ambient music", e)
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (ignored: Exception) {}
                audioTrack = null
            }
        }
    }

    fun stop() {
        if (!isPlaying) return
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
        
        launchOnMain {
            onCompletionCallback?.invoke()
        }
    }

    private fun launchOnMain(block: () -> Unit) {
        synthScope.launch(Dispatchers.Main) {
            block()
        }
    }
}
