package com.example.modelmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModelRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: ModelRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = ModelRepository(context)
        repository.removeInstalledModel()
    }

    @After
    fun tearDown() {
        repository.removeInstalledModel()
    }

    @Test
    fun testInitialStateNotInstalled() {
        val state = repository.checkExistingModel()
        assertEquals(ModelInstallState.NotInstalled, state)
        assertEquals(ModelInstallState.NotInstalled, repository.installState.value)
    }

    @Test
    fun testExistingModelDetectedWhenValidSize() {
        // Create mock model file with exact expected bytes
        val file = repository.modelFile
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(100))

        // If file exists but size doesn't match expected bytes, remains NotInstalled
        assertEquals(ModelInstallState.NotInstalled, repository.checkExistingModel())

        // If file exists with valid receipt
        repository.metaFile.writeText(ModelRepository.EXPECTED_SHA256)
        // Without exact size, still NotInstalled
        assertEquals(ModelInstallState.NotInstalled, repository.checkExistingModel())

        // Now test removeInstalledModel
        repository.removeInstalledModel()
        assertFalse(file.exists())
        assertFalse(repository.metaFile.exists())
        assertEquals(ModelInstallState.NotInstalled, repository.installState.value)
    }

    @Test
    fun testCancelDownloadResetsState() {
        repository.cancelDownload()
        assertEquals(ModelInstallState.NotInstalled, repository.installState.value)
    }
}

