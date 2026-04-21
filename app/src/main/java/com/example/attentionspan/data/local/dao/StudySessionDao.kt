package com.example.attentionspan.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.attentionspan.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insertSession(session: StudySession)

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY startTime DESC")
    fun getSessionsForSubject(subjectId: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Delete
    suspend fun deleteSession(session: StudySession)
}
