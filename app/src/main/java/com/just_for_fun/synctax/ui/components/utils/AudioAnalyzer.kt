package com.just_for_fun.synctax.ui.components.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioAnalyzer {
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate,
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    
    private var audioRecord: AudioRecord? = null
    private var isInitialized = false

    init {
        try {
            // Check if AudioRecord can be initialized
            if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize != AudioRecord.ERROR) {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                
                // Check if initialization was successful
                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                    isInitialized = true
                }
            }
        } catch (e: Exception) {
            // Permission denied or other initialization error
            isInitialized = false
            audioRecord?.release()
            audioRecord = null
        }
    }

    // Function to get amplitude data (simplified version)
//    fun getAmplitudeData(): FloatArray {
//        val amplitudeData = FloatArray(4) // Let's assume you have 4 bars to animate
//        var totalAmplitude: Int
//        var index = 0
//
//        // Read data from the audio buffer
//        audioRecord.read(audioBuffer, 0, bufferSize)
//
//        // Process the audio data to get amplitude (simplified)
//        for (i in 0 until amplitudeData.size) {
//            totalAmplitude = 0
//            for (j in 0 until (audioBuffer.size / amplitudeData.size)) {
//                totalAmplitude += audioBuffer[i * (audioBuffer.size / amplitudeData.size) + j].toInt()
//            }
//            // Normalize the amplitude and map it to a range (0.3f to 1f)
//            amplitudeData[i] = (log10(totalAmplitude.toFloat() + 1) * 0.3f).coerceIn(0.3f, 1f)
//        }
//
//        return amplitudeData
//    }

    fun getAmplitudeData(): FloatArray {
        return floatArrayOf(0.4f, 0.6f, 0.7f, 0.8f)
    }


    fun isReady(): Boolean = isInitialized

    // To stop recording
    fun stop() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            isInitialized = false
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
}
