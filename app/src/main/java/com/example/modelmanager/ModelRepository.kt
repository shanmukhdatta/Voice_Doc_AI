package com.example.modelmanager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface ModelInstallState {
    data object NotInstalled : ModelInstallState
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : ModelInstallState {
        val progress: Float
            get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    }
    data object Verifying : ModelInstallState
    data class Installed(val path: String, val sizeBytes: Long) : ModelInstallState
    data class Error(val message: String, val canRetry: Boolean) : ModelInstallState
}

interface IModelRepository {
    val installState: StateFlow<ModelInstallState>
    fun checkExistingModel(): ModelInstallState
    fun startDownload()
    fun cancelDownload()
    fun removeInstalledModel(): Boolean
}

open class ModelRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : IModelRepository {

    companion object {
        private const val TAG = "ModelRepository"

        const val MODEL_NAME = "Qwen3-0.6B-Q4_K_M"
        const val MODEL_FILENAME = "Qwen3-0.6B.Q4_K_M.gguf"
        const val MODEL_URL = "https://huggingface.co/QuantFactory/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B.Q4_K_M.gguf"
        const val EXPECTED_BYTES = 484_220_000L
        const val EXPECTED_SHA256 = "7af3fdf842f87b24672f8a7f1dd50404043f0bfb71093ff91c31d2b49df4631d"
    }

    private val modelsDir = File(context.filesDir, "models")
    val modelFile = File(modelsDir, MODEL_FILENAME)
    private val partialFile = File(modelsDir, "$MODEL_FILENAME.partial")

    private val _installState = MutableStateFlow<ModelInstallState>(ModelInstallState.NotInstalled)
    override val installState: StateFlow<ModelInstallState> = _installState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        checkExistingModel()
    }

    override fun checkExistingModel(): ModelInstallState {
        if (modelFile.exists() && modelFile.length() == EXPECTED_BYTES) {
            val state = ModelInstallState.Installed(modelFile.absolutePath, modelFile.length())
            _installState.value = state
            return state
        }
        val state = ModelInstallState.NotInstalled
        _installState.value = state
        return state
    }

    override fun startDownload() {
        if (_installState.value is ModelInstallState.Downloading ||
            _installState.value is ModelInstallState.Installed) {
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch {
            downloadModelInternal()
        }
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        if (partialFile.exists()) {
            partialFile.delete()
        }
        _installState.value = ModelInstallState.NotInstalled
    }

    override fun removeInstalledModel(): Boolean {
        downloadJob?.cancel()
        downloadJob = null
        if (partialFile.exists()) {
            partialFile.delete()
        }
        val deleted = if (modelFile.exists()) modelFile.delete() else true
        _installState.value = ModelInstallState.NotInstalled
        Log.i(TAG, "Model removed from local storage")
        return deleted
    }

    private suspend fun downloadModelInternal() = withContext(Dispatchers.IO) {
        try {
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }
            if (partialFile.exists()) {
                partialFile.delete()
            }

            _installState.value = ModelInstallState.Downloading(0L, EXPECTED_BYTES)
            Log.i(TAG, "Starting download from $MODEL_URL")

            val request = Request.Builder()
                .url(MODEL_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _installState.value = ModelInstallState.Error(
                        message = "Download failed (HTTP ${response.code}). Check connection.",
                        canRetry = true
                    )
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    _installState.value = ModelInstallState.Error(
                        message = "Empty response received from server.",
                        canRetry = true
                    )
                    return@withContext
                }

                val totalBytes = if (body.contentLength() > 0) body.contentLength() else EXPECTED_BYTES
                val digest = MessageDigest.getInstance("SHA-256")

                body.byteStream().use { input: InputStream ->
                    FileOutputStream(partialFile).use { output: FileOutputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var downloaded: Long = 0L
                        var lastEmit: Long = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            digest.update(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 200 || downloaded == totalBytes) {
                                lastEmit = now
                                _installState.value = ModelInstallState.Downloading(downloaded, totalBytes)
                            }
                        }
                        output.flush()
                    }
                }

                _installState.value = ModelInstallState.Verifying
                Log.i(TAG, "Download complete. Verifying SHA-256...")

                val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
                if (partialFile.length() != EXPECTED_BYTES || !sha256.equals(EXPECTED_SHA256, ignoreCase = true)) {
                    Log.e(TAG, "Verification mismatch! Length: ${partialFile.length()} (expected $EXPECTED_BYTES), SHA: $sha256 (expected $EXPECTED_SHA256)")
                    partialFile.delete()
                    _installState.value = ModelInstallState.Error(
                        message = "Downloaded model failed integrity verification.",
                        canRetry = true
                    )
                    return@withContext
                }

                // Atomic rename
                if (modelFile.exists()) {
                    modelFile.delete()
                }
                val renamed = partialFile.renameTo(modelFile)
                if (!renamed) {
                    // Fallback copy
                    partialFile.copyTo(modelFile, overwrite = true)
                    partialFile.delete()
                }

                Log.i(TAG, "Model successfully installed at ${modelFile.absolutePath}")
                _installState.value = ModelInstallState.Installed(modelFile.absolutePath, modelFile.length())
            }
        } catch (e: CancellationException) {
            Log.i(TAG, "Download cancelled by user")
            if (partialFile.exists()) {
                partialFile.delete()
            }
            _installState.value = ModelInstallState.NotInstalled
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            if (partialFile.exists()) {
                partialFile.delete()
            }
            _installState.value = ModelInstallState.Error(
                message = e.localizedMessage ?: "Network error during download",
                canRetry = true
            )
        }
    }
}
