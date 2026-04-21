package com.example.attentionspan.data.repository

import com.example.attentionspan.data.local.dao.StudySessionDao
import com.example.attentionspan.data.local.dao.SubjectDao
import com.example.attentionspan.data.local.entity.StudySession
import com.example.attentionspan.data.local.entity.Subject
import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val subjectDao: SubjectDao,
    private val studySessionDao: StudySessionDao
) {
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()
    val allSessions: Flow<List<StudySession>> = studySessionDao.getAllSessions()

    suspend fun insertSubject(subject: Subject) {
        subjectDao.insertSubject(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        subjectDao.deleteSubject(subject)
    }

    suspend fun getSubjectById(id: Long): Subject? {
        return subjectDao.getSubjectById(id)
    }

    suspend fun insertSession(session: StudySession) {
        studySessionDao.insertSession(session)
    }

    suspend fun deleteSession(session: StudySession) {
        studySessionDao.deleteSession(session)
    }

    fun getSessionsForSubject(subjectId: Long): Flow<List<StudySession>> {
        return studySessionDao.getSessionsForSubject(subjectId)
    }
}
