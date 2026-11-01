package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.model.DoctorUiState
import com.example.model.isListening
import com.example.viewmodel.VoiceDoctorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private fun createViewModel(): VoiceDoctorViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return VoiceDoctorViewModel(app)
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Voice Doctor AI", appName)
    }

    @Test
    fun `test initial state and sound toggle`() {
        val viewModel = createViewModel()
        val initial = viewModel.uiState.value
        assertEquals(AppLanguage.ENGLISH, initial.currentLanguage)
        assertTrue(initial.isSoundEnabled)
        assertEquals(2, initial.messages.size)

        // Toggle sound
        viewModel.toggleSound()
        assertFalse(viewModel.uiState.value.isSoundEnabled)
    }

    @Test
    fun `test language toggle updates messages and state`() {
        val viewModel = createViewModel()
        viewModel.setLanguage(AppLanguage.TELUGU)
        assertEquals(AppLanguage.TELUGU, viewModel.uiState.value.currentLanguage)
        assertTrue(viewModel.uiState.value.messages.first().text.contains("తలనొప్పి"))
    }

    @Test
    fun `test recording flow state transition in telugu demo mode`() {
        val viewModel = createViewModel()
        viewModel.setLanguage(AppLanguage.TELUGU)
        viewModel.startRecording()
        assertTrue(viewModel.uiState.value.uiState is DoctorUiState.RecordingDemo)
        assertTrue(viewModel.uiState.value.uiState.isListening)

        viewModel.cancelRecording()
        assertTrue(viewModel.uiState.value.uiState is DoctorUiState.Idle)
    }

    @Test
    fun `test clear and restore conversation`() {
        val viewModel = createViewModel()
        assertEquals(2, viewModel.uiState.value.messages.size)

        // Clear messages to trigger centered doctor layout
        viewModel.clearConversation()
        assertTrue(viewModel.uiState.value.messages.isEmpty())

        // Restore messages
        viewModel.loadSampleConversation()
        assertEquals(2, viewModel.uiState.value.messages.size)
    }
}
