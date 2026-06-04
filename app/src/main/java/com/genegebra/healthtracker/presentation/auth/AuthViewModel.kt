package com.genegebra.healthtracker.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genegebra.healthtracker.domain.model.SessionManager
import com.genegebra.healthtracker.domain.model.User
import com.genegebra.healthtracker.domain.repository.AuthRepository
import com.genegebra.healthtracker.domain.usecase.LoginUseCase
import com.genegebra.healthtracker.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegisterMode: Boolean = false,
    val pendingEmailVerification: Boolean = false,
    val pendingEmail: String = "",
    val loggedInUser: User? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleMode() {
        _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loginUseCase(email, password)
                .onSuccess { user ->
                    sessionManager.newSession()
                    if (!user.isEmailVerified) {
                        _uiState.update { it.copy(isLoading = false, pendingEmailVerification = true, pendingEmail = email) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, loggedInUser = user) }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    // reCAPTCHA token left empty — email verification is the human check.
    // Wire in reCAPTCHA here once a backend is available to verify the token.
    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            registerUseCase(email, password, "")
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, pendingEmailVerification = true, isRegisterMode = false, pendingEmail = email)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            authRepository.sendVerificationEmail()
        }
    }

    fun checkEmailVerified() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.reloadUser()
                .onSuccess { user ->
                    if (user.isEmailVerified) {
                        _uiState.update { it.copy(isLoading = false, pendingEmailVerification = false, loggedInUser = user) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Email not yet verified. Check your inbox.") }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun setError(message: String) = _uiState.update { it.copy(error = message) }

    fun isValidEmail(email: String): Boolean = Companion.isValidEmail(email)

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
    }
}
