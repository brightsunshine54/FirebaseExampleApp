package ru.filantrop.firebaseexampleapp.util

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result) { _, _, _ -> }
        } else {
            continuation.resumeWithException(
                task.exception ?: RuntimeException("Task failed")
            )
        }
    }
}
