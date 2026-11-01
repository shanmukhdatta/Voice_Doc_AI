package com.example.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecorderTest {

    private val recorder = AudioRecorder()

    @Test
    fun testPcm16ToNormalizedFloatConversion() {
        val pcmSamples = shortArrayOf(
            0,
            32767,
            -32768,
            16384,
            -16384
        )

        val floats = recorder.convertPcm16ToNormalizedFloat(pcmSamples)

        assertEquals(pcmSamples.size, floats.size)
        assertEquals(0.0f, floats[0], 0.0001f)
        assertEquals(32767f / 32768f, floats[1], 0.0001f)
        assertEquals(-1.0f, floats[2], 0.0001f)
        assertEquals(0.5f, floats[3], 0.0001f)
        assertEquals(-0.5f, floats[4], 0.0001f)

        for (sample in floats) {
            assertTrue("Sample $sample must be >= -1.0", sample >= -1.0f)
            assertTrue("Sample $sample must be <= 1.0", sample <= 1.0f)
        }
    }

    @Test
    fun testInitialState() {
        assertFalse(recorder.isRecording.value)
        assertEquals(0f, recorder.amplitude.value, 0.0001f)
    }

    @Test
    fun testSafeCancelWhenIdle() {
        recorder.cancelRecording()
        assertFalse(recorder.isRecording.value)
        assertEquals(0f, recorder.amplitude.value, 0.0001f)
    }

    @Test
    fun testStopWhenIdleReturnsEmptyArray() {
        val samples = recorder.stopRecording()
        assertEquals(0, samples.size)
        assertFalse(recorder.isRecording.value)
    }

    @Test
    fun testMultipleSequentialReleasesDoNotThrow() {
        recorder.release()
        recorder.release()
        recorder.release()
        assertFalse(recorder.isRecording.value)
    }
}

