package ru.filantrop.firebaseexampleapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import ru.filantrop.firebaseexampleapp.util.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        firebaseAuth.addAuthStateListener(listener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        firebaseAuth.removeAuthStateListener(listener)
    }

    suspend fun registerWithEmail(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user)
    }

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user)
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun sendEmailVerification() {
        val user = requireNotNull(firebaseAuth.currentUser) { "Нет текущего пользователя" }
        user.sendEmailVerification().await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    suspend fun reloadCurrentUser(): FirebaseUser? {
        val user = firebaseAuth.currentUser ?: return null
        user.reload().await()
        return user
    }
}
