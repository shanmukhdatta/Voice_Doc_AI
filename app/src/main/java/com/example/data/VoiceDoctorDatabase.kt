package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VoiceDoctorDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceDoctorDatabase? = null

        fun getInstance(context: Context): VoiceDoctorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VoiceDoctorDatabase::class.java,
                    "voice_doctor_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

