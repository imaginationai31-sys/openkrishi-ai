package ai.openkrishi.app

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.ClearCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

object FirebaseAuthManager {
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun signUp(email: String, password: String, onResult: (Result<FirebaseUser>) -> Unit) {
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

    fun signIn(email: String, password: String, onResult: (Result<FirebaseUser>) -> Unit) {
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

    fun signInAsGuest(onResult: (Result<FirebaseUser>) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.user?.let { onResult(Result.success(it)) }
                        ?: onResult(Result.failure(IllegalStateException("No guest session returned.")))
                } else {
                    onResult(Result.failure(task.exception ?: Exception("Guest sign-in failed.")))
                }
            }
    }

    fun signInWithGoogle(context: Context, onResult: (Result<FirebaseUser>) -> Unit) {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(com.google.firebase.auth.R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        credentialManager.getCredentialAsync(
            context,
            request,
            androidx.core.os.CancellationSignal(),
            java.util.concurrent.Executors.newSingleThreadExecutor(),
            object : androidx.credentials.CredentialManagerCallback<androidx.credentials.GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException> {
                override fun onResult(result: androidx.credentials.GetCredentialResponse) {
                    handleGoogleCredential(result.credential, onResult)
                }
                override fun onError(e: androidx.credentials.exceptions.GetCredentialException) {
                    onResult(Result.failure(e))
                }
            }
        )
    }

    private fun handleGoogleCredential(credential: Credential, onResult: (Result<FirebaseUser>) -> Unit) {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            onResult(Result.failure(IllegalArgumentException("Google credential was not returned.")))
            return
        }

        try {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.user?.let { onResult(Result.success(it)) }
                            ?: onResult(Result.failure(IllegalStateException("No Google user session returned.")))
                    } else {
                        onResult(Result.failure(task.exception ?: Exception("Google sign-in failed.")))
                    }
                }
        } catch (e: GoogleIdTokenParsingException) {
            onResult(Result.failure(e))
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

    fun signOut(context: Context? = null) {
        auth.signOut()
        if (context != null) {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialStateAsync(
                ClearCredentialStateRequest(),
                androidx.core.os.CancellationSignal(),
                java.util.concurrent.Executors.newSingleThreadExecutor(),
                object : androidx.credentials.CredentialManagerCallback<Unit, ClearCredentialException> {
                    override fun onResult(result: Unit) = Unit
                    override fun onError(e: ClearCredentialException) = Unit
                }
            )
        }
    }
}
