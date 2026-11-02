package com.example.rag

import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VectorRetriever(
    embeddingsFile: File,
    passageIdsFile: File
) {
    val passageCount: Int
    val dimension: Int
    private val passageIds: List<String>
    private val vectors: FloatArray

    init {
        val idsJson = passageIdsFile.readText(Charsets.UTF_8)
        val jsonArray = JSONArray(idsJson)
        val idsList = ArrayList<String>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            idsList.add(jsonArray.getString(i))
        }
        passageIds = idsList

        FileInputStream(embeddingsFile).use { fis ->
            val headerBytes = ByteArray(12)
            var read = 0
            while (read < 12) {
                val r = fis.read(headerBytes, read, 12 - read)
                if (r < 0) break
                read += r
            }
            val headerBuf = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(4)
            headerBuf.get(magic)
            val magicStr = String(magic, Charsets.US_ASCII)
            require(magicStr == "VEC1") { "Invalid vector file magic: $magicStr" }

            passageCount = headerBuf.getInt()
            dimension = headerBuf.getInt()
            require(passageCount == passageIds.size) {
                "Passage count mismatch: bin has $passageCount, ids file has ${passageIds.size}"
            }

            val totalFloats = passageCount * dimension
            val totalBytes = totalFloats * 4
            val floatBytes = ByteArray(totalBytes)
            var bytesRead = 0
            while (bytesRead < totalBytes) {
                val r = fis.read(floatBytes, bytesRead, totalBytes - bytesRead)
                if (r < 0) break
                bytesRead += r
            }

            val floatBuf = ByteBuffer.wrap(floatBytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            vectors = FloatArray(totalFloats)
            floatBuf.get(vectors)
        }
    }

    fun search(queryVector: FloatArray, topK: Int = 5): List<Pair<String, Float>> {
        require(queryVector.size == dimension) {
            "Query vector dimension ${queryVector.size} != index dimension $dimension"
        }

        val scores = ArrayList<Pair<String, Float>>(passageCount)
        for (i in 0 until passageCount) {
            val offset = i * dimension
            var dot = 0f
            for (d in 0 until dimension) {
                dot += queryVector[d] * vectors[offset + d]
            }
            scores.add(Pair(passageIds[i], dot))
        }

        // Sort descending by score
        scores.sortByDescending { it.second }
        return scores.take(topK)
    }
}
