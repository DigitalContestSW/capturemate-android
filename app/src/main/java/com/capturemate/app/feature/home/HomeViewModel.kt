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
                _uiState.update { it.copy(session = session, isSessionLoaded = true) }
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
                        errorMessage = throwable.toGoogleLoginMessage(),
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
                it.copy(
                    isLoading = false,
                    isSessionLoaded = true,
                    session = null,
                    errorMessage = null,
                )
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
    val isSessionLoaded: Boolean = false,
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

private fun Throwable.toGoogleLoginMessage(): String {
    val rawMessage = message.orEmpty()
    return when {
        rawMessage.contains("No credentials available", ignoreCase = true) -> {
            "기기에 로그인된 Google 계정이 없거나, 현재 기기에서 사용할 수 있는 자격 증명이 없습니다. Google 계정이 로그인된 Android 기기나 Google Play 지원 에뮬레이터에서 다시 시도하세요."
        }
        rawMessage.isNotBlank() -> rawMessage
        else -> "Google login failed."
    }
}
