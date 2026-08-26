package com.moondicine.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.database.entity.UserProfileEntity
import com.moondicine.app.data.database.entity.UserStatsEntity
import com.moondicine.app.data.repository.QuestionRepository
import com.moondicine.app.data.repository.SupabaseSyncRepository
import com.moondicine.app.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalQuestions: Int = 0,
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val accuracy: Float = 0f,
    val currentStreak: Int = 0,
    val specialties: List<String> = emptyList(),
    val stats: List<UserStatsEntity> = emptyList(),
    val userProfile: UserProfileEntity? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository,
    private val supabaseSyncRepository: SupabaseSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadDashboardData(syncRemote = true) }
    }

    private suspend fun loadDashboardData(syncRemote: Boolean) {
        if (syncRemote) {
            supabaseSyncRepository.syncQuestionBank()
        }

        // Load user profile
        val profile = userProgressRepository.getUserProfile()

        // Load question count
        val totalQuestions = questionRepository.getQuestionCount()

        // Load stats
        val answered = userProgressRepository.getTotalAnswered()
        val correct = userProgressRepository.getTotalCorrect()
        val accuracy = if (answered > 0) correct.toFloat() / answered.toFloat() else 0f

        // Load specialties
        val specialties = questionRepository.getAllSpecialtiesList()

        // Load detailed stats
        val stats = userProgressRepository.getAllStats()

        _uiState.update {
            it.copy(
                isLoading = false,
                totalQuestions = totalQuestions,
                totalAnswered = answered,
                totalCorrect = correct,
                accuracy = accuracy,
                currentStreak = profile?.currentStreakDays ?: 0,
                specialties = specialties,
                stats = stats,
                userProfile = profile
            )
        }
    }

    fun syncNow() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            val result = supabaseSyncRepository.syncQuestionBank()
            loadDashboardData(syncRemote = false)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = result.fold(
                        onSuccess = { count -> "Sincronizadas $count questões" },
                        onFailure = { error -> "Sincronização indisponível: ${error.message}" }
                    )
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { loadDashboardData(syncRemote = false) }
    }
}
