package com.example.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import kotlin.math.max
import kotlin.math.sqrt

class MiniLmOnnxEmbeddingEngine(
    private val modelFile: File,
    private val tokenizer: WordPieceTokenizer,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : TextEmbeddingEngine, Closeable {

    override val dimension: Int = 384

    private val environment: OrtEnvironment by lazy {
        OrtEnvironment.getEnvironment()
    }
    private val sessionOptions: OrtSession.SessionOptions by lazy {
        OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
        }
    }
    private val session: OrtSession by lazy {
        environment.createSession(modelFile.absolutePath, sessionOptions)
    }

    override suspend fun embed(text: String): FloatArray = withContext(dispatcher) {
        val tokenized = tokenizer.tokenize(text)
        val seqLen = tokenized.inputIds.size

        val inputIds2D = arrayOf(tokenized.inputIds)
        val attentionMask2D = arrayOf(tokenized.attentionMask)
        val tokenTypeIds2D = arrayOf(tokenized.tokenTypeIds)

        val inputIdsTensor = OnnxTensor.createTensor(environment, inputIds2D)
        val attentionMaskTensor = OnnxTensor.createTensor(environment, attentionMask2D)
        val tokenTypeIdsTensor = OnnxTensor.createTensor(environment, tokenTypeIds2D)

        try {
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )

            session.run(inputs).use { results ->
                val outputValue = results.get("last_hidden_state").orElseGet { results.first().value }
                val outputTensor = outputValue as OnnxTensor

                @Suppress("UNCHECKED_CAST")
                val rawHiddenStates = outputTensor.value as Array<Array<FloatArray>>
                val tokenVectors = rawHiddenStates[0]

                val pooled = FloatArray(dimension)
                var activeTokens = 0f

                for (t in 0 until seqLen) {
                    if (tokenized.attentionMask[t] == 1L) {
                        activeTokens += 1f
                        val hidden = tokenVectors[t]
                        for (d in 0 until dimension) {
                            pooled[d] += hidden[d]
                        }
                    }
                }

                val divisor = max(activeTokens, 1e-9f)
                for (d in 0 until dimension) {
                    pooled[d] /= divisor
                }

                // L2 normalization
                var sumSq = 0.0
                for (d in 0 until dimension) {
                    sumSq += (pooled[d] * pooled[d]).toDouble()
                }
                val norm = max(sqrt(sumSq).toFloat(), 1e-9f)
                for (d in 0 until dimension) {
                    pooled[d] /= norm
                }

                pooled
            }
        } finally {
            inputIdsTensor.close()
            attentionMaskTensor.close()
            tokenTypeIdsTensor.close()
        }
    }

    override fun close() {
        try {
            session.close()
        } catch (_: Throwable) {}
        try {
            sessionOptions.close()
        } catch (_: Throwable) {}
        try {
            environment.close()
        } catch (_: Throwable) {}
    }
}
