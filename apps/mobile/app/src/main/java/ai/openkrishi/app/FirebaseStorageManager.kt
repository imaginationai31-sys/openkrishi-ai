package ai.openkrishi.app

import android.graphics.Bitmap
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

internal object FirebaseStorageManager {
    private val storage = FirebaseStorage.getInstance()

    fun uploadCropImage(
        user: FirebaseUser,
        bitmap: Bitmap,
        onResult: (Result<String>) -> Unit
    ) {
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val ref = storage.reference
            .child("users")
            .child(user.uid)
            .child("crop-images")
            .child(filename)

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        val bytes = output.toByteArray()

        ref.putBytes(bytes)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: IllegalStateException("Image upload failed.")
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { uri -> onResult(Result.success(uri.toString())) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}
