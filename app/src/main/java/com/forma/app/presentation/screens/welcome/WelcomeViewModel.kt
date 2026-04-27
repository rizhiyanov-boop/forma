package com.forma.app.presentation.screens.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.UserProfileRepository
import com.forma.app.presets.MyProgramPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelcomeUi(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val profileRepo: UserProfileRepository,
    private val programRepo: ProgramRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(WelcomeUi())
    val ui: StateFlow<WelcomeUi> = _ui.asStateFlow()

    fun loadPreset(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                profileRepo.saveProfile(MyProgramPreset.profile)
                programRepo.loadPresetProgram()
                _ui.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (t: Throwable) {
                android.util.Log.e("Forma", "Failed to load preset", t)
                _ui.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Не удалось загрузить программу: ${t.message}"
                    )
                }
            }
        }
    }
}
