package com.forma.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.Program
import com.forma.app.domain.model.Workout
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUi(
    val isLoading: Boolean = true,
    val program: Program? = null,
    val today: Workout? = null,
    val thisWeekSessions: List<WorkoutSession> = emptyList(),
    val streak: Int = 0,
    val volumeKg: Double = 0.0,
    val weekday: DayOfWeek = DayOfWeek.MON
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val programRepo: ProgramRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    private val weekStart: Long = run {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            // Понедельник как начало
            val shift = (get(Calendar.DAY_OF_WEEK) + 5) % 7
            add(Calendar.DAY_OF_MONTH, -shift)
        }
        cal.timeInMillis
    }

    val ui: StateFlow<HomeUi> = combine(
        programRepo.observeActiveProgram(),
        sessionRepo.observeSessionsSince(weekStart)
    ) { program, sessions ->
        val today = currentDayOfWeek()
        val todaysWorkout = program?.workouts?.firstOrNull { it.dayOfWeek == today }
        val volume = sessions.sumOf { it.totalVolumeKg }
        val streak = computeStreak(sessions)
        HomeUi(
            isLoading = false,
            program = program,
            today = todaysWorkout,
            thisWeekSessions = sessions,
            streak = streak,
            volumeKg = volume,
            weekday = today
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())

    fun startSession(workoutId: String, programId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val session = sessionRepo.createSession(workoutId, programId)
            onReady(session.id)
        }
    }

    private fun currentDayOfWeek(): DayOfWeek {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> DayOfWeek.MON
            Calendar.TUESDAY -> DayOfWeek.TUE
            Calendar.WEDNESDAY -> DayOfWeek.WED
            Calendar.THURSDAY -> DayOfWeek.THU
            Calendar.FRIDAY -> DayOfWeek.FRI
            Calendar.SATURDAY -> DayOfWeek.SAT
            else -> DayOfWeek.SUN
        }
    }

    /**
     * Считаем число последовательных дней с завершёнными тренировками,
     * заканчивающихся сегодня или вчера.
     */
    private fun computeStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        val finishedDays = sessions
            .filter { it.isFinished }
            .map { startOfDay(it.finishedAt ?: it.startedAt) }
            .toSortedSet(reverseOrder())

        if (finishedDays.isEmpty()) return 0
        val today = startOfDay(System.currentTimeMillis())
        val yesterday = today - ONE_DAY_MS

        if (finishedDays.first() != today && finishedDays.first() != yesterday) return 0

        var streak = 1
        var cursor = finishedDays.first()
        for (d in finishedDays.drop(1)) {
            if (cursor - d == ONE_DAY_MS) {
                streak++; cursor = d
            } else break
        }
        return streak
    }

    private fun startOfDay(ts: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    companion object {
        private const val ONE_DAY_MS = 86_400_000L
    }
}
