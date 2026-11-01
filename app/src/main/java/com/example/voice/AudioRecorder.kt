package com.example.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

interface VoiceRecorder {
    val isRecording: StateFlow<Boolean>
    val amplitude: StateFlow<Float>
    fun startRecording(scope: CoroutineScope, onMaxDurationReached: (() -> Unit)? = null): Result<Unit>
    fun stopRecording(): FloatArray
    fun cancelRecording()
    fun release()
}

/**
 * AudioRecorder captures 16 kHz Mono PCM 16-bit audio from the device microphone
 * and converts it into a normalized FloatArray for Whisper.cpp inference.
 */
open class AudioRecorder(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : VoiceRecorder {
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val MAX_RECORDING_SECONDS = 30
        const val MAX_SAMPLES = SAMPLE_RATE * MAX_RECORDING_SECONDS
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    override val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    // Thread-safe buffer for accumulating recorded samples
    private val pcmBuffer = mutableListOf<Short>()
    private val bufferLock = Any()

    /**
     * Starts audio recording in a background coroutine.
     */
    @SuppressLint("MissingPermission")
    override fun startRecording(
        scope: CoroutineScope,
        onMaxDurationReached: (() -> Unit)?
    ): Result<Unit> {
        if (_isRecording.value) {
            Log.w(TAG, "Recording already in progress.")
            return Result.success(Unit)
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            val errorMsg = "Invalid buffer size computed: $minBufferSize"
            Log.e(TAG, errorMsg)
            return Result.failure(IllegalStateException(errorMsg))
        }

        val bufferSize = (minBufferSize * 2).coerceAtLeast(SAMPLE_RATE / 2)

        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                val errorMsg = "AudioRecord failed to initialize. State: ${record.state}"
                Log.e(TAG, errorMsg)
                return Result.failure(IllegalStateException(errorMsg))
            }

            synchronized(bufferLock) {
                pcmBuffer.clear()
            }
            _amplitude.value = 0f
            audioRecord = record

            record.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch(dispatcher) {
                val readBuffer = ShortArray(1024)
                var totalSamplesCaptured = 0

                try {
                    while (isActive && _isRecording.value && totalSamplesCaptured < MAX_SAMPLES) {
                        val samplesRead = record.read(readBuffer, 0, readBuffer.size)
                        if (samplesRead > 0) {
                            var sumSquare = 0.0
                            synchronized(bufferLock) {
                                for (i in 0 until samplesRead) {
                                    if (totalSamplesCaptured + i < MAX_SAMPLES) {
                                        pcmBuffer.add(readBuffer[i])
                                        val sampleVal = readBuffer[i].toDouble()
                                        sumSquare += sampleVal * sampleVal
                                    }
                                }
                            }
                            totalSamplesCaptured += samplesRead

                            val rms = sqrt(sumSquare / samplesRead)
                            val normalizedAmp = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                            _amplitude.value = normalizedAmp
                        } else if (samplesRead < 0) {
                            Log.e(TAG, "Error reading audio data: $samplesRead")
                            break
                        }
                    }

                    if (totalSamplesCaptured >= MAX_SAMPLES) {
                        Log.i(TAG, "Max recording duration of $MAX_RECORDING_SECONDS seconds reached.")
                        withContext(Dispatchers.Main) {
                            onMaxDurationReached?.invoke()
                        }
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Recording job cancelled.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in recording loop", e)
                } finally {
                    cleanUpNativeRecord()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
            cleanUpNativeRecord()
            Result.failure(e)
        }
    }

    /**
     * Stops recording and returns the accumulated samples as a normalized FloatArray in [-1.0f, 1.0f].
     */
    override fun stopRecording(): FloatArray {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        cleanUpNativeRecord()

        val captured: ShortArray
        synchronized(bufferLock) {
            captured = pcmBuffer.toShortArray()
            pcmBuffer.clear()
        }
        _amplitude.value = 0f

        return convertPcm16ToNormalizedFloat(captured)
    }

    /**
     * Cancels recording and discards all captured samples.
     */
    override fun cancelRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        cleanUpNativeRecord()

        synchronized(bufferLock) {
            pcmBuffer.clear()
        }
        _amplitude.value = 0f
    }

    override fun release() {
        cancelRecording()
    }

    private fun cleanUpNativeRecord() {
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        } finally {
            audioRecord = null
            _isRecording.value = false
        }
    }

    fun convertPcm16ToNormalizedFloat(pcmSamples: ShortArray): FloatArray {
        val floatSamples = FloatArray(pcmSamples.size)
        for (i in pcmSamples.indices) {
            floatSamples[i] = pcmSamples[i] / 32768.0f
        }
        return floatSamples
    }
}
