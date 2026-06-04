package com.genegebra.healthtracker.presentation.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genegebra.healthtracker.domain.model.HealthEntry
import com.genegebra.healthtracker.domain.model.SessionManager
import com.genegebra.healthtracker.domain.model.User
import com.genegebra.healthtracker.domain.repository.AuthRepository
import com.genegebra.healthtracker.domain.usecase.GetEntryForSessionUseCase
import com.genegebra.healthtracker.domain.usecase.SaveEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryUiState(
    val isLoading: Boolean = true,
    val currentUser: User? = null,
    val existingEntry: HealthEntry? = null,
    val isSaved: Boolean = false,
    val error: String? = null,
    // Admin: list of all users to pick from
    val allUsers: List<User> = emptyList(),
    val selectedUserId: String? = null
)

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val saveEntryUseCase: SaveEntryUseCase,
    private val getEntryForSessionUseCase: GetEntryForSessionUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.filterNotNull().first().let { user ->
                _uiState.update { it.copy(currentUser = user) }
                val targetUserId = user.uid
                val existing = getEntryForSessionUseCase(targetUserId, sessionManager.sessionId)
                _uiState.update { it.copy(isLoading = false, existingEntry = existing) }
            }
        }
    }

    fun selectUserForAdmin(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedUserId = userId) }
            val existing = getEntryForSessionUseCase(userId, sessionManager.sessionId)
            _uiState.update { it.copy(isLoading = false, existingEntry = existing) }
        }
    }

    fun resetSession() {
        sessionManager.newSession()
        _uiState.update { it.copy(existingEntry = null, isSaved = false, error = null) }
    }

    fun saveEntry(
        systolic: String,
        diastolic: String,
        pulse: String,
        anxietyLevel: String
    ) {
        val user = _uiState.value.currentUser ?: return
        val targetUserId = _uiState.value.selectedUserId ?: user.uid

        val entry = HealthEntry(
            userId = targetUserId,
            userEmail = user.email,
            sessionId = sessionManager.sessionId,
            systolic = systolic.toIntOrNull(),
            diastolic = diastolic.toIntOrNull(),
            pulse = pulse.toIntOrNull(),
            anxietyLevel = anxietyLevel.toIntOrNull()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            saveEntryUseCase(entry)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true, existingEntry = entry) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
