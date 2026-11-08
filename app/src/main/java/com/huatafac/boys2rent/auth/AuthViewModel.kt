package com.huatafac.boys2rent.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: FirebaseUser) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(
        if (authRepository.currentUser != null) {
            AuthState.Authenticated(authRepository.currentUser!!)
        } else {
            AuthState.Unauthenticated
        }
    )
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(false)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = authRepository.authStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentUser
        )

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                if (user != null) {
                    if (_uiState.value !is AuthState.Authenticated) {
                        _uiState.value = AuthState.Authenticated(user)
                    }
                } else {
                    if (_uiState.value is AuthState.Authenticated || _uiState.value is AuthState.Loading) {
                        _uiState.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _email.value = email
    }

    fun onPasswordChanged(password: String) {
        _password.value = password
    }

    fun toggleAuthMode() {
        _isSignUpMode.value = !_isSignUpMode.value
    }

    fun signInWithEmail() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value
        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _uiState.value = AuthState.Error("Por favor, introduza o e-mail e a palavra-passe")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            val result = authRepository.signInWithEmailAndPassword(currentEmail, currentPassword)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    if (user.isEmailVerified) {
                        _uiState.value = AuthState.Authenticated(user)
                    } else {
                        _uiState.value = AuthState.Error("Por favor, confirme o seu e-mail antes de entrar. Verifique a sua caixa de entrada.")
                    }
                } else {
                    _uiState.value = AuthState.Error("Falha no início de sessão: Objeto de utilizador nulo")
                }
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.localizedMessage ?: "Falha na autenticação")
            }
        }
    }

    fun signUpWithEmail() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value
        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _uiState.value = AuthState.Error("Por favor, introduza o e-mail e a palavra-passe")
            return
        }
        if (currentPassword.length < 6) {
            _uiState.value = AuthState.Error("A palavra-passe deve ter pelo menos 6 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            val result = authRepository.createUserWithEmailAndPassword(currentEmail, currentPassword)
            if (result.isSuccess) {
                _uiState.value = AuthState.Error("Conta criada com sucesso! Por favor, confirme o seu e-mail antes de entrar. Verifique a sua caixa de entrada.")
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.localizedMessage ?: "Falha no registo")
            }
        }
    }

    /**
     * Envia o e-mail de recuperação da palavra-passe para [email].
     * [onResult] recebe a mensagem de erro, ou null se o pedido foi aceite.
     */
    fun sendPasswordReset(email: String, onResult: (String?) -> Unit) {
        val limpo = email.trim()
        if (limpo.isBlank()) {
            onResult("Indica o teu e-mail.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(limpo).matches()) {
            onResult("O e-mail não é válido.")
            return
        }
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(limpo)
            onResult(
                when (val e = result.exceptionOrNull()) {
                    null -> null
                    is com.google.firebase.FirebaseNetworkException -> "Sem ligação à internet. Tenta outra vez."
                    is com.google.firebase.FirebaseTooManyRequestsException -> "Demasiados pedidos. Espera um pouco e tenta outra vez."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "O e-mail não é válido."
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Não existe nenhuma conta com esse e-mail."
                    else -> e.localizedMessage ?: "Não foi possível enviar o e-mail."
                }
            )
        }
    }

    fun resendVerificationEmail() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value
        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _uiState.value = AuthState.Error("Por favor, introduza o e-mail e a palavra-passe para reenviar o e-mail de confirmação.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            val result = authRepository.resendVerificationEmail(currentEmail, currentPassword)
            if (result.isSuccess) {
                _uiState.value = AuthState.Error("E-mail de confirmação reenviado com sucesso! Verifique a sua caixa de entrada.")
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.localizedMessage ?: "Falha ao reenviar e-mail de confirmação")
            }
        }
    }

    fun signInWithGoogle(context: Context, webClientId: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    val signInResult = authRepository.signInWithGoogleCredential(idToken)
                    if (signInResult.isSuccess) {
                        val user = signInResult.getOrNull()
                        if (user != null) {
                            _uiState.value = AuthState.Authenticated(user)
                        } else {
                            _uiState.value = AuthState.Error("Falha no início de sessão: Objeto de utilizador nulo")
                        }
                    } else {
                        _uiState.value = AuthState.Error(signInResult.exceptionOrNull()?.localizedMessage ?: "Falha na autenticação")
                    }
                } else {
                    _uiState.value = AuthState.Error("Tipo de credencial inesperado recebido")
                }
            } catch (e: GetCredentialException) {
                val message = e.localizedMessage ?: "Início de sessão com Google cancelado ou falhou"
                if (message.contains("cancelled", ignoreCase = true)) {
                    _uiState.value = if (authRepository.currentUser != null) {
                        AuthState.Authenticated(authRepository.currentUser!!)
                    } else {
                        AuthState.Unauthenticated
                    }
                } else {
                    _uiState.value = AuthState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.localizedMessage ?: "Ocorreu um erro desconhecido")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            authRepository.signOut()
            _uiState.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_uiState.value is AuthState.Error) {
            _uiState.value = if (authRepository.currentUser != null) {
                AuthState.Authenticated(authRepository.currentUser!!)
            } else {
                AuthState.Unauthenticated
            }
        }
    }
}
