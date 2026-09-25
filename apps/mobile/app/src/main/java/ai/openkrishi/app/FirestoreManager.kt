package ai.openkrishi.app

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

internal data class DiagnosisHistoryItem(
    val id: String = "",
    val type: String = "advisory",
    val cropCategory: String = "",
    val cropName: String = "",
    val language: String = "",
    val query: String = "",
    val answer: String = "",
    val observations: List<String> = emptyList(),
    val possibleCauses: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val confidence: String = "",
    val safety: String = "",
    val imageSource: String = "",
    val imageUrl: String = "",
    val createdAtMillis: Long = 0L
)

internal object FirestoreManager {
    private val db: FirebaseFirestore = Firebase.firestore

    private fun userRef(user: FirebaseUser) = db.collection("users").document(user.uid)
    private fun historyRef(user: FirebaseUser) = userRef(user).collection("diagnosisHistory")

    fun saveFarmerProfile(
        user: FirebaseUser,
        displayName: String?,
        language: String,
        cropCategory: String? = null,
        cropName: String? = null,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        val data = hashMapOf<String, Any>(
            "uid" to user.uid,
            "language" to language,
            "updatedAt" to System.currentTimeMillis()
        )
        displayName?.takeIf { it.isNotBlank() }?.let { data["displayName"] = it }
        user.email?.let { data["email"] = it }
        user.phoneNumber?.let { data["phone"] = it }
        cropCategory?.takeIf { it.isNotBlank() }?.let { data["cropCategory"] = it }
        cropName?.takeIf { it.isNotBlank() }?.let { data["cropName"] = it }

        userRef(user).set(data, SetOptions.merge())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun saveDiagnosis(
        user: FirebaseUser,
        type: String,
        language: String,
        cropCategory: String,
        cropName: String,
        query: String? = null,
        result: AdvisoryResult,
        imageSource: String? = null,
        imageUrl: String? = null,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        val doc = historyRef(user).document()
        val data = hashMapOf<String, Any>(
            "type" to type,
            "language" to language,
            "cropCategory" to cropCategory,
            "answer" to result.answer,
            "confidence" to result.confidence,
            "safety" to result.safety,
            "observations" to result.observations,
            "possibleCauses" to result.possibleCauses,
            "recommendations" to result.recommendations,
            "createdAt" to System.currentTimeMillis()
        )
        if (cropName.isNotBlank()) data["cropName"] = cropName
        query?.takeIf { it.isNotBlank() }?.let { data["query"] = it }
        imageSource?.takeIf { it.isNotBlank() }?.let { data["imageSource"] = it }
        imageUrl?.takeIf { it.isNotBlank() }?.let { data["imageUrl"] = it }

        doc.set(data)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun loadHistory(
        user: FirebaseUser,
        onResult: (Result<List<DiagnosisHistoryItem>>) -> Unit
    ) {
        historyRef(user)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.map { doc ->
                    DiagnosisHistoryItem(
                        id = doc.id,
                        type = doc.getString("type") ?: "advisory",
                        cropCategory = doc.getString("cropCategory") ?: "",
                        cropName = doc.getString("cropName") ?: "",
                        language = doc.getString("language") ?: "",
                        query = doc.getString("query") ?: "",
                        answer = doc.getString("answer") ?: "",
                        observations = doc.get("observations") as? List<String> ?: emptyList(),
                        possibleCauses = doc.get("possibleCauses") as? List<String> ?: emptyList(),
                        recommendations = doc.get("recommendations") as? List<String> ?: emptyList(),
                        confidence = doc.getString("confidence") ?: "",
                        safety = doc.getString("safety") ?: "",
                        imageSource = doc.getString("imageSource") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        createdAtMillis = doc.getLong("createdAt") ?: 0L
                    )
                }
                onResult(Result.success(items))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}
