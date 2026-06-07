package com.just_for_fun.synctax.core.party

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sin

/**
 * Handles the "Chirp Test" to measure physical speaker-to-microphone latency.
 */
class AcousticCalibrator {

    companion object {
        private const val TAG = "AcousticCalibrator"
        private const val SAMPLE_RATE = 44100
        private const val CHIRP_FREQ = 18000.0 // 18kHz (ultrasonic/high frequency)
        private const val CHIRP_DURATION_MS = 50
    }

    /**
     * Plays a high-frequency chirp for a short duration.
     */
    suspend fun playChirp() = withContext(Dispatchers.IO) {
        val numSamples = (CHIRP_DURATION_MS * SAMPLE_RATE) / 1000
        val generatedSnd = ByteArray(2 * numSamples)
        
        for (i in 0 until numSamples) {
            val dVal = sin(2.0 * Math.PI * i.toDouble() * CHIRP_FREQ / SAMPLE_RATE)
            val val16 = (dVal * 32767).toInt().toShort()
            generatedSnd[2 * i] = (val16.toInt() and 0x00ff).toByte()
            generatedSnd[2 * i + 1] = ((val16.toInt() and 0xff00) ushr 8).toByte()
        }

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            generatedSnd.size,
            AudioTrack.MODE_STATIC
        )

        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        Log.d(TAG, "🔊 Playing Chirp (${CHIRP_FREQ}Hz)")
        audioTrack.play()
        
        delay(CHIRP_DURATION_MS.toLong() + 100) // Wait for playback to finish
        audioTrack.release()
    }

    /**
     * Listens for the chirp and returns the exact elapsed time (in ms) when it was detected.
     * Returns -1 if timeout occurs or permissions are missing.
     */
    @SuppressLint("MissingPermission")
    suspend fun listenForChirp(timeoutMs: Long = 5000L): Long = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "AudioRecord buffer size error")
            return@withContext -1L
        }

        val recorder: AudioRecord
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing RECORD_AUDIO permission", e)
            return@withContext -1L
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            return@withContext -1L
        }

        val buffer = ShortArray(bufferSize)
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "👂 Listening for Chirp...")
        
        recorder.startRecording()

        var detectedTime = -1L
        var isListening = true

        while (isListening && (System.currentTimeMillis() - startTime) < timeoutMs) {
            val read = recorder.read(buffer, 0, buffer.size)
            if (read > 0) {
                // Simple peak detection for high frequency
                var maxAmplitude = 0
                for (i in 0 until read) {
                    if (abs(buffer[i].toInt()) > maxAmplitude) {
                        maxAmplitude = abs(buffer[i].toInt())
                    }
                }
                
                // Threshold for "chirp" detection (can be tuned)
                if (maxAmplitude > 15000) { 
                    detectedTime = System.currentTimeMillis()
                    Log.d(TAG, "✅ Chirp detected at $detectedTime (amplitude: $maxAmplitude)")
                    isListening = false
                }
            }
        }

        recorder.stop()
        recorder.release()

        if (detectedTime == -1L) {
            Log.w(TAG, "⏱️ Timeout listening for chirp")
        }

        return@withContext detectedTime
    }
}
