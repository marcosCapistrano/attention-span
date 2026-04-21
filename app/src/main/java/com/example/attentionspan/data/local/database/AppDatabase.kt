package com.example.attentionspan.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.attentionspan.data.local.dao.StudySessionDao
import com.example.attentionspan.data.local.dao.SubjectDao
import com.example.attentionspan.data.local.entity.StudySession
import com.example.attentionspan.data.local.entity.Subject

@Database(entities = [Subject::class, StudySession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attention_span_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
