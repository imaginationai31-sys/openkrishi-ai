package ai.openkrishi.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object FirebaseAuthManager {
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun signUp(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        if (email.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Email is required.")))
            return
        }
        if (password.length < 6) {
            onResult(Result.failure(IllegalArgumentException("Password must be at least 6 characters.")))
            return
        }
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.user?.let { onResult(Result.success(it)) }
                        ?: onResult(Result.failure(IllegalStateException("No user session returned.")))
                } else {
                    onResult(Result.failure(task.exception ?: Exception("Authentication failed.")))
                }
            }
    }

    fun signIn(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        if (email.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Email is required.")))
            return
        }
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.user?.let { onResult(Result.success(it)) }
                        ?: onResult(Result.failure(IllegalStateException("No user session returned.")))
                } else {
                    onResult(Result.failure(task.exception ?: Exception("Sign-in failed.")))
                }
            }
    }

    fun sendPasswordReset(email: String, onResult: (Result<Unit>) -> Unit) {
        if (email.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Email is required.")))
            return
        }
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Password reset failed.")))
            }
    }

    fun signOut() {
        auth.signOut()
    }
}
