package ai.openkrishi.app

import android.graphics.Bitmap
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

internal object FirebaseAiManager {
    private const val MODEL_NAME = "gemini-3.8-flash"

    private val model by lazy {
        FirebaseAI.getInstance(GenerativeBackend.googleAI()).generativeModel(MODEL_NAME)
    }

    fun advisory(query: String, language: String, cropCategory: String, cropName: String?): AdvisoryResult = runBlocking {
        val prompt = """
            You are OpenKrishi AI, a cautious agricultural advisory assistant for Indian farmers.
            Return ONLY valid JSON. Do not use markdown or code fences.
            Selected language: $language. The response MUST be entirely in that language with no mixed English.
            Crop category: $cropCategory. Crop name/variety: ${cropName.orEmpty()}.
            User problem: $query
            JSON keys: answer, confidence, safety, observations, possible_causes, recommendations.
            Do not invent a confirmed disease from symptoms alone. Avoid exact pesticide or fertilizer doses unless the cause is established.
        """.trimIndent()
        parseAdvisory(model.generateContent(prompt).text, cropCategory)
    }

    fun vision(bitmap: Bitmap, language: String, cropCategory: String, growthStage: String?, cropName: String?): AdvisoryResult = runBlocking {
        val prompt = """
            You are OpenKrishi AI, a cautious crop-vision agronomy assistant for Indian farmers.
            Analyze the attached crop image and metadata.
            Selected language: $language. Entire user-facing output MUST be in that language only, with no mixed English.
            Crop category: $cropCategory. Crop name/variety: ${cropName.orEmpty()}. Growth stage: ${growthStage.orEmpty()}.
            Return ONLY valid JSON with keys: answer, confidence, safety, observations, possible_causes, recommendations.
            Observations must describe visible symptoms only. Possible causes are plausible, not definitive.
            If the image is unclear, request a clearer close-up photo. Do not invent a disease.
            Avoid exact pesticide or fertilizer doses unless evidence is strong.
        """.trimIndent()
        val promptContent = content { image(bitmap); text(prompt) }
        parseAdvisory(model.generateContent(promptContent).text, cropCategory)
    }

    private fun parseAdvisory(raw: String?, cropCategory: String): AdvisoryResult {
        val cleaned = raw.orEmpty().trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        fun list(key: String): List<String> {
            val array = json.optJSONArray(key) ?: return emptyList()
            return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        }
        return AdvisoryResult(
            answer = json.optString("answer", "পর্যাপ্ত তথ্য পাওয়া যায়নি।"),
            confidence = json.optString("confidence", "low"),
            safety = json.optString("safety", "caution"),
            observations = list("observations"),
            possibleCauses = list("possible_causes"),
            recommendations = list("recommendations"),
            cropCategory = cropCategory
        )
    }
}