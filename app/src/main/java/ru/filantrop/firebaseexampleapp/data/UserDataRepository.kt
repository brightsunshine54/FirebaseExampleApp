package ru.filantrop.firebaseexampleapp.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import ru.filantrop.firebaseexampleapp.util.await

class UserDataRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun fetchSecret(uid: String): String? {
        val snapshot: DocumentSnapshot =
            firestore.collection("users").document(uid).get().await()
        if (!snapshot.exists()) {
            return null
        }
        return snapshot.getString("secret")
    }
}
