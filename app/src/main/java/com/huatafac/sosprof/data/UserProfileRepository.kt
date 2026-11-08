package com.huatafac.sosprof.data

import com.google.firebase.database.FirebaseDatabase
import com.huatafac.sosprof.model.UserProfile
import com.huatafac.sosprof.model.toFirebaseMap
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val usersRef = database.getReference("users")

    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val snapshot = usersRef.child(uid).get().await()
            val profile = snapshot.getValue(UserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Grava só os campos da foto do perfil (cópia local e versão na nuvem). */
    suspend fun guardarFotoPerfil(uid: String, profileImageUri: String, versao: Long): Result<Unit> = try {
        usersRef.child(uid).updateChildren(
            mapOf<String, Any>("profileImageUri" to profileImageUri, "fotoPerfilAtualizada" to versao)
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun saveUserProfile(uid: String, profile: UserProfile): Result<Unit> {
        return try {
            // updateChildren e não setValue: users/{uid} também guarda turmas e horário,
            // e um setValue no nó inteiro apagava-os sempre que se gravava o perfil.
            usersRef.child(uid).updateChildren(profile.toFirebaseMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
