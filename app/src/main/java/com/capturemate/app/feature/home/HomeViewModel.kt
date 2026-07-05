package com.capturemate.app.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.capturemate.app.domain.model.AuthSession
import com.capturemate.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                _uiState.update { it.copy(session = session) }
            }
        }
        viewModelScope.launch {
            authRepository.observeOnboardingCompleted().collect { completed ->
                _uiState.update { it.copy(onboardingCompleted = completed) }
            }
        }
    }

    fun signIn(context: Context) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                authRepository.signInWithGoogle(context)
            }.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Google login failed.",
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching {
                authRepository.signOut()
            }
            _uiState.update {
                HomeUiState(onboardingCompleted = it.onboardingCompleted)
            }
        }
    }

    fun completeOnboarding() {
        _uiState.update { it.copy(onboardingCompleted = true) }
        viewModelScope.launch {
            authRepository.completeOnboarding()
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val session: AuthSession? = null,
    val onboardingCompleted: Boolean = false,
    val errorMessage: String? = null,
)

class HomeViewModelFactory(
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(authRepository) as T
    }
}
