package com.moondicine.app.ui.screens.specialties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpecialtyItem(
    val name: String,
    val questionCount: Int
)

data class SpecialtyBrowserUiState(
    val isLoading: Boolean = true,
    val specialties: List<SpecialtyItem> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class SpecialtyBrowserViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpecialtyBrowserUiState())
    val uiState: StateFlow<SpecialtyBrowserUiState> = _uiState.asStateFlow()

    init {
        loadSpecialties()
    }

    fun loadSpecialties() {
        viewModelScope.launch {
            runCatching {
                questionRepository.getAllSpecialtiesList().map { specialty ->
                    SpecialtyItem(
                        name = specialty,
                        questionCount = questionRepository.getQuestionsBySpecialty(specialty).size
                    )
                }
            }.onSuccess { specialties ->
                _uiState.value = SpecialtyBrowserUiState(
                    isLoading = false,
                    specialties = specialties
                )
            }.onFailure { error ->
                _uiState.value = SpecialtyBrowserUiState(
                    isLoading = false,
                    errorMessage = "Não foi possível carregar as especialidades: ${error.message}"
                )
            }
        }
    }
}
