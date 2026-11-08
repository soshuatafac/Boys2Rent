package com.huatafac.boys2rent.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        trySend(firebaseAuth.currentUser)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Firebase user is null after Google sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                if (user.isEmailVerified) {
                    Result.success(user)
                } else {
                    try {
                        user.sendEmailVerification().await()
                    } catch (e: Exception) {
                        // ignore or log
                    }
                    firebaseAuth.signOut()
                    Result.failure(IllegalStateException("Por favor, confirme o seu e-mail antes de entrar. Verifique a sua caixa de entrada."))
                }
            } else {
                Result.failure(IllegalStateException("Firebase user is null after email sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pede ao Firebase o e-mail para definir uma nova palavra-passe, em português.
     * Por segurança, o Firebase pode responder com sucesso mesmo que não exista conta com esse e-mail.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        firebaseAuth.setLanguageCode("pt-PT")
        firebaseAuth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                user.sendEmailVerification().await()
                firebaseAuth.signOut()
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Firebase user is null after account creation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerificationEmail(email: String, password: String): Result<Unit> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                if (!user.isEmailVerified) {
                    user.sendEmailVerification().await()
                    firebaseAuth.signOut()
                    Result.success(Unit)
                } else {
                    firebaseAuth.signOut()
                    Result.failure(IllegalStateException("Este e-mail já se encontra verificado."))
                }
            } else {
                Result.failure(IllegalStateException("Utilizador não encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            // Log or ignore
        }
    }
}
