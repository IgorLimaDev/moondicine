package com.moondicine.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.database.entity.UserProfileEntity
import com.moondicine.app.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 3,
    val name: String = "",
    val targetSpecialty: String = "",
    val experienceLevel: String = "",
    val isCompleting: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateTargetSpecialty(specialty: String) {
        _uiState.update { it.copy(targetSpecialty = specialty) }
    }

    fun updateExperienceLevel(level: String) {
        _uiState.update { it.copy(experienceLevel = level) }
    }

    fun nextStep() {
        _uiState.update {
            it.copy(currentStep = minOf(it.currentStep + 1, it.totalSteps - 1))
        }
    }

    fun previousStep() {
        _uiState.update {
            it.copy(currentStep = maxOf(it.currentStep - 1, 0))
        }
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }

            val state = _uiState.value
            val profile = UserProfileEntity(
                id = 1,
                name = state.name,
                targetSpecialty = state.targetSpecialty,
                experienceLevel = state.experienceLevel,
                onboardingCompleted = true
            )

            userProgressRepository.saveUserProfile(profile)
            _uiState.update { it.copy(isCompleting = false) }
            onCompleted()
        }
    }

    fun canProceed(): Boolean {
        val state = _uiState.value
        return when (state.currentStep) {
            0 -> state.name.isNotBlank()
            1 -> state.targetSpecialty.isNotBlank()
            2 -> state.experienceLevel.isNotBlank()
            else -> false
        }
    }
}
