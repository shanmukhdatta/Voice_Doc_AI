package com.example.rag

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.Closeable
import java.io.File

class MedicalKnowledgeStore(dbFile: File) : Closeable {

    companion object {
        private const val TAG = "MedicalKnowledgeStore"
    }

    private val db: SQLiteDatabase = SQLiteDatabase.openDatabase(
        dbFile.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
    )

    fun getPassage(passageId: String): EvidencePassage? {
        val cursor = db.rawQuery(
            "SELECT passage_id, topic_id, title, url, text FROM passages WHERE passage_id = ? LIMIT 1",
            arrayOf(passageId)
        )
        return cursor.use {
            if (it.moveToFirst()) {
                parsePassage(it)
            } else {
                null
            }
        }
    }

    fun getPassages(passageIds: List<String>): Map<String, EvidencePassage> {
        if (passageIds.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, EvidencePassage>()
        val placeholders = passageIds.joinToString(",") { "?" }
        val cursor = db.rawQuery(
            "SELECT passage_id, topic_id, title, url, text FROM passages WHERE passage_id IN ($placeholders)",
            passageIds.toTypedArray()
        )
        cursor.use {
            while (it.moveToNext()) {
                val p = parsePassage(it)
                result[p.id] = p
            }
        }
        return result
    }

    fun searchFts(query: String, limit: Int = 10): List<EvidencePassage> {
        val sanitizedQuery = sanitizeFtsQuery(query)
        if (sanitizedQuery.isBlank()) return emptyList()

        val results = mutableListOf<EvidencePassage>()
        try {
            val cursor = db.rawQuery(
                """
                SELECT p.passage_id, p.topic_id, p.title, p.url, p.text 
                FROM passages_fts f 
                JOIN passages p ON f.passage_id = p.passage_id 
                WHERE passages_fts MATCH ? 
                LIMIT ?
                """.trimIndent(),
                arrayOf(sanitizedQuery, limit.toString())
            )
            cursor.use {
                while (it.moveToNext()) {
                    results.add(parsePassage(it))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FTS search failed for query: '$sanitizedQuery'", e)
        }
        return results
    }

    private fun parsePassage(cursor: Cursor): EvidencePassage {
        val id = cursor.getString(0)
        val topicId = cursor.getString(1)
        val title = cursor.getString(2)
        val url = cursor.getString(3)
        val text = cursor.getString(4)
        return EvidencePassage(
            id = id,
            topicId = topicId,
            title = title,
            section = "Summary",
            text = text,
            sourceUrl = url,
            sourceAttribution = "MedlinePlus, National Library of Medicine (NIH)"
        )
    }

    private fun sanitizeFtsQuery(raw: String): String {
        val tokens = raw.replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
        if (tokens.isEmpty()) return ""
        // FTS match format: "token1* OR token2*"
        return tokens.take(8).joinToString(" OR ") { "$it*" }
    }

    override fun close() {
        try {
            db.close()
        } catch (_: Exception) {}
    }
}
