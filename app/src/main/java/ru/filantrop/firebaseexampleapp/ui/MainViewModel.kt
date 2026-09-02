package ru.filantrop.firebaseexampleapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.filantrop.firebaseexampleapp.data.AuthRepository
import ru.filantrop.firebaseexampleapp.data.UserDataRepository

sealed interface AuthUiState {
    data object Unknown : AuthUiState
    data object SignedOut : AuthUiState
    data class SignedIn(val user: FirebaseUser) : AuthUiState
}

data class MainUiState(
    val authState: AuthUiState = AuthUiState.Unknown,
    val isAuthenticating: Boolean = false,
    val loginError: String? = null,
    val isSecretLoading: Boolean = false,
    val secret: String? = null,
    val secretError: String? = null,
    val isEmailVerified: Boolean? = null,
    val isSendingVerification: Boolean = false,
    val verificationMessage: String? = null,
    val isSendingPasswordReset: Boolean = false,
    val passwordResetMessage: String? = null,
    val passwordResetError: String? = null,
)

class MainViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userDataRepository = UserDataRepository()

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        onAuthStateChanged(auth.currentUser)
    }

    init {
        authRepository.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        authRepository.removeAuthStateListener(authStateListener)
    }

    private fun onAuthStateChanged(user: FirebaseUser?) {
        if (user == null) {
            _state.update {
                MainUiState(authState = AuthUiState.SignedOut)
            }
            return
        }
        _state.update {
            it.copy(
                authState = AuthUiState.SignedIn(user),
                isAuthenticating = false,
                loginError = null,
                isEmailVerified = user.isEmailVerified(),
            )
        }
        loadSecret(user.uid)
        refreshEmailVerification()
    }

    fun loginWithEmail(email: String, password: String) {
        _state.update { it.copy(isAuthenticating = true, loginError = null) }
        viewModelScope.launch {
            try {
                authRepository.signInWithEmail(email, password)
            } catch (error: Exception) {
                _state.update {
                    it.copy(isAuthenticating = false, loginError = friendlyMessage(error))
                }
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        _state.update { it.copy(isAuthenticating = true, loginError = null) }
        viewModelScope.launch {
            try {
                val user = authRepository.registerWithEmail(email, password)
                try {
                    authRepository.sendEmailVerification()
                } catch (_: Exception) {
                    // Письмо о подтверждении не критично для создания аккаунта
                }
                _state.update {
                    it.copy(
                        isAuthenticating = false,
                        loginError = null,
                        isEmailVerified = user.isEmailVerified(),
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(isAuthenticating = false, loginError = friendlyMessage(error))
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun sendPasswordReset(email: String) {
        _state.update {
            it.copy(
                isSendingPasswordReset = true,
                passwordResetMessage = null,
                passwordResetError = null,
            )
        }
        viewModelScope.launch {
            try {
                authRepository.sendPasswordResetEmail(email)
                _state.update {
                    it.copy(
                        isSendingPasswordReset = false,
                        passwordResetMessage = "Письмо для сброса пароля отправлено на $email",
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isSendingPasswordReset = false,
                        passwordResetError = friendlyMessage(error),
                    )
                }
            }
        }
    }

    fun resendEmailVerification() {
        val email = (state.value.authState as? AuthUiState.SignedIn)?.user?.email
        _state.update {
            it.copy(isSendingVerification = true, verificationMessage = null)
        }
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification()
                val target = email ?: ""
                _state.update {
                    it.copy(
                        isSendingVerification = false,
                        verificationMessage =
                            if (target.isBlank())
                                "Письмо для подтверждения email отправлено"
                            else
                                "Письмо для подтверждения email отправлено на $target",
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isSendingVerification = false,
                        verificationMessage = friendlyMessage(error),
                    )
                }
            }
        }
    }

    fun retryLoadSecret() {
        (state.value.authState as? AuthUiState.SignedIn)
            ?.let { loadSecret(it.user.uid) }
    }

    private fun refreshEmailVerification() {
        viewModelScope.launch {
            try {
                val reloaded = authRepository.reloadCurrentUser()
                if (reloaded != null) {
                    _state.update {
                        it.copy(
                            authState = AuthUiState.SignedIn(reloaded),
                            isEmailVerified = reloaded.isEmailVerified(),
                        )
                    }
                }
            } catch (_: Exception) {
                // Ничего: используем уже известное состояние
            }
        }
    }

    private fun loadSecret(uid: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(isSecretLoading = true, secret = null, secretError = null)
            }
            try {
                val secret = userDataRepository.fetchSecret(uid)
                _state.update {
                    if (secret.isNullOrEmpty()) {
                        it.copy(
                            isSecretLoading = false,
                            secret = null,
                            secretError = "Для этого пользователя строка ещё не сохранена в Firestore",
                        )
                    } else {
                        it.copy(isSecretLoading = false, secret = secret)
                    }
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isSecretLoading = false,
                        secretError = error.message ?: "Не удалось загрузить строку",
                    )
                }
            }
        }
    }

    private fun friendlyMessage(error: Exception): String = when (error) {
        is FirebaseAuthException -> when (error.errorCode) {
            "auth/invalid-email" -> "Некорректный email"
            "auth/invalid-credential",
            "auth/invalid-login-credentials",
            "auth/invalid-api-key",
            "auth/user-not-found",
            "auth/wrong-password" -> "Неверный email или пароль"
            "auth/email-already-in-use" -> "Пользователь с таким email уже зарегистрирован"
            "auth/weak-password" -> "Пароль слишком слабый, минимум 6 символов"
            "auth/too-many-requests" -> "Слишком много попыток, попробуйте позже"
            "auth/network-request-failed" -> "Ошибка сети, проверьте подключение"
            else -> error.message ?: "Произошла ошибка, попробуйте позже"
        }

        else -> error.message ?: "Произошла ошибка, попробуйте позже"
    }
}
