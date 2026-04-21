package com.example.attentionspan.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.*
import com.example.attentionspan.data.local.database.AppDatabase
import com.example.attentionspan.data.local.entity.StudySession
import com.example.attentionspan.data.local.entity.Subject
import com.example.attentionspan.data.repository.StudyRepository
import com.example.attentionspan.service.TimerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.sqrt

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StudyRepository
    val allSubjects: Flow<List<Subject>>
    val allSessions: Flow<List<StudySession>>

    val overviewStats: Flow<OverviewStats>
    val subjectStatistics: Flow<List<SubjectWithStats>>

    private var timerService: TimerService? = null
    private var isBound = false

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true
            
            viewModelScope.launch {
                timerService?.isRunning?.collect { _timerRunning.value = it }
            }
            viewModelScope.launch {
                timerService?.elapsedTime?.collect { _elapsedTime.value = it }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudyRepository(database.subjectDao(), database.studySessionDao())
        allSubjects = repository.allSubjects
        allSessions = repository.allSessions

        val intent = Intent(application, TimerService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        overviewStats = allSessions.map { sessions ->
            calculateOverviewStats(sessions)
        }
        // ... rest of init

        subjectStatistics = combine(allSubjects, allSessions) { subjects, sessions ->
            val overallAverage = if (sessions.isEmpty()) 0.0 else sessions.map { it.durationMillis.toDouble() / 60000 }.average()
            val durations = sessions.map { it.durationMillis.toDouble() / 60000 }
            val stdDev = if (durations.size < 2) 0.0 else {
                val mean = durations.average()
                sqrt(durations.map { (it - mean) * (it - mean) }.average())
            }

            subjects.map { subject ->
                val subjectSessions = sessions.filter { it.subjectId == subject.id }.sortedBy { it.startTime }
                calculateSubjectStats(subject, subjectSessions, overallAverage, stdDev)
            }
        }
    }

    private fun calculateOverviewStats(sessions: List<StudySession>): OverviewStats {
        if (sessions.isEmpty()) return OverviewStats()

        val durationsMinutes = sessions.map { it.durationMillis.toDouble() / 60000 }
        val avg = durationsMinutes.average()
        val best = durationsMinutes.maxOrNull() ?: 0.0
        val total = sessions.size

        // Rolling 7-day average
        val calendar = Calendar.getInstance()
        val rolling7Day = mutableListOf<Pair<Long, Double>>()
        
        // Simplified rolling average for chart: last 30 days
        for (i in 0 until 30) {
            val endOfDay = Calendar.getInstance().apply { 
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis
            val startOf7DaysAgo = Calendar.getInstance().apply {
                timeInMillis = endOfDay
                add(Calendar.DAY_OF_YEAR, -7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val weekSessions = sessions.filter { it.startTime in startOf7DaysAgo..endOfDay }
            if (weekSessions.isNotEmpty()) {
                val weekAvg = weekSessions.map { it.durationMillis.toDouble() / 60000 }.average()
                rolling7Day.add(endOfDay to weekAvg)
            }
        }

        return OverviewStats(
            averageMinutes = avg,
            bestSessionMinutes = best,
            totalSessions = total,
            rolling7DayAverage = rolling7Day.reversed()
        )
    }

    private fun calculateSubjectStats(
        subject: Subject,
        sessions: List<StudySession>,
        overallAverage: Double,
        stdDev: Double
    ): SubjectWithStats {
        if (sessions.isEmpty()) {
            return SubjectWithStats(subject, 0.0, 0.0, 0, emptyList(), FocusBadge.AVERAGE)
        }

        val durations = sessions.map { it.durationMillis.toDouble() / 60000 }
        val avg = durations.average()
        val best = durations.maxOrNull() ?: 0.0
        
        val badge = when {
            avg > overallAverage + stdDev -> FocusBadge.FOCUSING_WELL
            avg < overallAverage - stdDev -> FocusBadge.HARDER_TO_FOCUS
            else -> FocusBadge.AVERAGE
        }

        // Simple trend: comparing last half of sessions to first half
        val trend = if (sessions.size >= 2) {
            val midpoint = sessions.size / 2
            val firstHalfAvg = durations.take(midpoint).average()
            val secondHalfAvg = durations.drop(midpoint).average()
            secondHalfAvg - firstHalfAvg
        } else 0.0

        return SubjectWithStats(
            subject = subject,
            averageMinutes = avg,
            bestMinutes = best,
            sessionCount = sessions.size,
            recentSessions = sessions.reversed().take(5),
            badge = badge,
            trend = trend,
            history = durations
        )
    }

    fun insertSubject(name: String) {
        viewModelScope.launch {
            repository.insertSubject(Subject(name = name))
        }
    }

    fun deleteSession(session: StudySession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun startTimer(subjectId: Long, subjectName: String) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            putExtra(TimerService.EXTRA_SUBJECT_ID, subjectId)
            putExtra(TimerService.EXTRA_SUBJECT_NAME, subjectName)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopTimer(subjectId: Long) {
        val duration = _elapsedTime.value
        val startTime = System.currentTimeMillis() - duration
        
        timerService?.stopTimer()
        
        viewModelScope.launch {
            repository.insertSession(
                StudySession(
                    subjectId = subjectId,
                    startTime = startTime,
                    durationMillis = duration
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }

    fun getSessionsForSubject(subjectId: Long): Flow<List<StudySession>> {
        return repository.getSessionsForSubject(subjectId)
    }

    fun getStatsForSubject(sessions: List<StudySession>): StudyStats {
        if (sessions.isEmpty()) return StudyStats()
        
        val totalDuration = sessions.sumOf { it.durationMillis }
        val averageDuration = totalDuration / sessions.size
        
        val now = Calendar.getInstance()
        val today = sessions.filter { 
            val sessionDate = Calendar.getInstance().apply { timeInMillis = it.startTime }
            sessionDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            sessionDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.durationMillis }
        
        return StudyStats(
            totalDuration = totalDuration,
            averageDuration = averageDuration,
            todayDuration = today
        )
    }
}

data class OverviewStats(
    val averageMinutes: Double = 0.0,
    val bestSessionMinutes: Double = 0.0,
    val totalSessions: Int = 0,
    val rolling7DayAverage: List<Pair<Long, Double>> = emptyList()
)

data class SubjectWithStats(
    val subject: Subject,
    val averageMinutes: Double,
    val bestMinutes: Double,
    val sessionCount: Int,
    val recentSessions: List<StudySession>,
    val badge: FocusBadge,
    val trend: Double = 0.0,
    val history: List<Double> = emptyList()
)

enum class FocusBadge {
    FOCUSING_WELL, AVERAGE, HARDER_TO_FOCUS
}

data class StudyStats(
    val totalDuration: Long = 0,
    val averageDuration: Long = 0,
    val todayDuration: Long = 0
)
