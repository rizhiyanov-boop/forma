package com.forma.app.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.ExperienceLevel
import com.forma.app.domain.model.Goal
import com.forma.app.domain.model.Sex
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUi(
    val step: Int = 0,                    // 0..6
    val goal: Goal? = null,
    val sex: Sex? = null,
    val level: ExperienceLevel? = null,
    val daysPerWeek: Int = 3,
    val preferredDays: Set<DayOfWeek> = emptySet(),
    val equipment: Set<Equipment> = emptySet(),
    val heightCm: String = "",
    val weightKg: String = "",
    val age: String = "",
    val limitations: String = "",
    val sessionDurationMin: Int = 60,

    val isGenerating: Boolean = false,
    val errorMessage: String? = null
) {
    val totalSteps = 7

    val canProceed: Boolean get() = when (step) {
        0 -> goal != null
        1 -> sex != null
        2 -> level != null
        3 -> daysPerWeek in 1..7
        4 -> equipment.isNotEmpty()
        5 -> true    // антропометрия опциональна
        6 -> true    // финальный шаг
        else -> false
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepo: UserProfileRepository,
    private val programRepo: ProgramRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUi())
    val ui: StateFlow<OnboardingUi> = _ui.asStateFlow()

    fun setGoal(g: Goal) = _ui.update { it.copy(goal = g) }
    fun setSex(x: Sex) = _ui.update { it.copy(sex = x) }
    fun setLevel(l: ExperienceLevel) = _ui.update { it.copy(level = l) }
    fun setDays(n: Int) = _ui.update { it.copy(daysPerWeek = n) }
    fun togglePreferredDay(d: DayOfWeek) = _ui.update {
        val s = it.preferredDays.toMutableSet().apply {
            if (!add(d)) remove(d)
        }
        it.copy(preferredDays = s)
    }
    fun toggleEquipment(e: Equipment) = _ui.update {
        val s = it.equipment.toMutableSet().apply { if (!add(e)) remove(e) }
        it.copy(equipment = s)
    }
    fun setHeight(v: String) = _ui.update { it.copy(heightCm = v.filter { c -> c.isDigit() }) }
    fun setWeight(v: String) = _ui.update { it.copy(weightKg = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    fun setAge(v: String) = _ui.update { it.copy(age = v.filter { c -> c.isDigit() }) }
    fun setLimitations(v: String) = _ui.update { it.copy(limitations = v) }
    fun setDuration(v: Int) = _ui.update { it.copy(sessionDurationMin = v) }

    fun next() = _ui.update { it.copy(step = (it.step + 1).coerceAtMost(it.totalSteps - 1)) }
    fun prev() = _ui.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun finish(onSuccess: () -> Unit) {
        val s = _ui.value
        val profile = UserProfile(
            goal = s.goal ?: return,
            level = s.level ?: return,
            sex = s.sex ?: Sex.UNSPECIFIED,
            daysPerWeek = s.daysPerWeek,
            preferredDays = s.preferredDays.toList().sortedBy { it.ordinal },
            equipment = s.equipment.toList(),
            heightCm = s.heightCm.toIntOrNull(),
            weightKg = s.weightKg.replace(',', '.').toDoubleOrNull(),
            age = s.age.toIntOrNull(),
            limitations = s.limitations.ifBlank { null },
            sessionDurationMin = s.sessionDurationMin
        )

        viewModelScope.launch {
            _ui.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                android.util.Log.d("Forma", "Starting program generation for profile: $profile")
                // Важно: профиль сохраняем ТОЛЬКО после успешной генерации программы.
                // Иначе при ошибке генерации навигация уйдёт на Home с пустой программой.
                val program = programRepo.generateAndSave(profile)
                android.util.Log.d("Forma", "Program generated: ${program.name}, workouts=${program.workouts.size}")
                profileRepo.saveProfile(profile)
                _ui.update { it.copy(isGenerating = false) }
                onSuccess()
            } catch (t: Throwable) {
                android.util.Log.e("Forma", "Program generation failed", t)
                _ui.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = formatError(t)
                    )
                }
            }
        }
    }

    private fun formatError(t: Throwable): String {
        // Делаем сообщение для пользователя информативнее
        val msg = t.message.orEmpty()
        return when {
            msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) ->
                "Неверный OpenAI API ключ. Проверь local.properties и пересобери проект."
            msg.contains("429") || msg.contains("rate", ignoreCase = true) ->
                "OpenAI вернул лимит запросов. Попробуй через минуту."
            msg.contains("timeout", ignoreCase = true) || msg.contains("timed out", ignoreCase = true) ->
                "Сервер OpenAI не ответил вовремя. Проверь интернет и повтори."
            msg.contains("Unable to resolve host", ignoreCase = true) ->
                "Нет интернета на устройстве."
            msg.contains("HTTP 5") ->
                "Сервер OpenAI временно недоступен. Попробуй ещё раз."
            else -> "Не удалось сгенерировать программу: ${msg.take(120)}"
        }
    }
}
