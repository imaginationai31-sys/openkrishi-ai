package ai.openkrishi.app

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

internal object FirebaseAiManager {

    private const val MODEL_NAME = "gemini-3.8-flash"

    private val model by lazy {
        Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel(MODEL_NAME)
    }

    fun advisory(
        query: String,
        language: String,
        cropCategory: String,
        cropName: String?
    ): AdvisoryResult = runBlocking {

        val prompt = """
            You are OpenKrishi AI, a cautious agricultural advisory assistant for Indian farmers.

            IMPORTANT LANGUAGE RULE:
            The selected language is "$language".
            ALL user-facing text must be written completely in that language.
            Do NOT mix English with the selected language.
            JSON field names must remain exactly as specified below.

            Crop category: $cropCategory
            Crop name/variety: ${cropName.orEmpty()}
            Farmer's problem: $query

            Return ONLY valid JSON.
            Do not use markdown.
            Do not use code fences.

            Required JSON structure:
            {
              "answer": "string",
              "confidence": "high|medium|low",
              "safety": "safe|caution|urgent",
              "observations": ["string"],
              "possible_causes": ["string"],
              "recommendations": ["string"]
            }

            Agricultural safety:
            - Do not claim a disease is confirmed from symptoms alone.
            - Possible causes must be presented as possibilities.
            - Do not recommend exact pesticide or fertilizer doses unless the cause is sufficiently established.
            - If information is insufficient, clearly say what additional information is needed.
        """.trimIndent()

        val raw = model.generateContent(prompt).text

        parseAdvisory(
            raw = raw,
            cropCategory = cropCategory,
            language = language
        )
    }

    fun vision(
        bitmap: Bitmap,
        language: String,
        cropCategory: String,
        growthStage: String?,
        cropName: String?
    ): AdvisoryResult = runBlocking {

        val prompt = """
            You are OpenKrishi AI, a cautious crop-vision agronomy assistant for Indian farmers.

            Analyze the attached crop image together with the supplied metadata.

            IMPORTANT LANGUAGE RULE:
            The selected language is "$language".
            ALL user-facing text must be written completely in that language.
            Do NOT mix English with the selected language.
            JSON field names must remain exactly as specified below.

            Crop category: $cropCategory
            Crop name/variety: ${cropName.orEmpty()}
            Growth stage: ${growthStage.orEmpty()}

            Return ONLY valid JSON.
            Do not use markdown.
            Do not use code fences.

            Required JSON structure:
            {
              "answer": "string",
              "confidence": "high|medium|low",
              "safety": "safe|caution|urgent",
              "observations": ["string"],
              "possible_causes": ["string"],
              "recommendations": ["string"]
            }

            Image analysis rules:
            - Observations must describe only symptoms that are actually visible.
            - Do not invent symptoms.
            - Possible causes are possibilities, not confirmed diagnoses.
            - Never claim a disease is confirmed from the image alone.
            - If the image is unclear, explicitly request a clearer close-up photograph.
            - Avoid exact pesticide or fertilizer doses unless evidence is strong.
        """.trimIndent()

        val promptContent = content {
            image(bitmap)
            text(prompt)
        }

        val raw = model.generateContent(promptContent).text

        parseAdvisory(
            raw = raw,
            cropCategory = cropCategory,
            language = language
        )
    }

    private fun parseAdvisory(
        raw: String?,
        cropCategory: String,
        language: String
    ): AdvisoryResult {

        val cleaned = cleanJsonResponse(raw)

        return try {
            val json = JSONObject(cleaned)

            AdvisoryResult(
                answer = json.optString(
                    "answer",
                    fallbackAnswer(language)
                ),
                confidence = json.optString(
                    "confidence",
                    "low"
                ),
                safety = json.optString(
                    "safety",
                    "caution"
                ),
                observations = getStringList(
                    json,
                    "observations"
                ),
                possibleCauses = getStringList(
                    json,
                    "possible_causes"
                ),
                recommendations = getStringList(
                    json,
                    "recommendations"
                ),
                cropCategory = cropCategory
            )

        } catch (e: Exception) {

            AdvisoryResult(
                answer = fallbackAnswer(language),
                confidence = "low",
                safety = "caution",
                observations = emptyList(),
                possibleCauses = emptyList(),
                recommendations = emptyList(),
                cropCategory = cropCategory
            )
        }
    }

    private fun cleanJsonResponse(raw: String?): String {

        var cleaned = raw
            .orEmpty()
            .trim()

        // Remove markdown code fences if Gemini adds them.
        cleaned = cleaned
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // If Gemini accidentally adds text before/after the JSON,
        // extract the outermost JSON object.
        val start = cleaned.indexOf("{")
        val end = cleaned.lastIndexOf("}")

        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1)
        }

        return cleaned
    }

    private fun getStringList(
        json: JSONObject,
        key: String
    ): List<String> {

        val array: JSONArray =
            json.optJSONArray(key) ?: return emptyList()

        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i).trim()

                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }
    }

    private fun fallbackAnswer(language: String): String {

        return when (language.lowercase()) {

            "bn" ->
                "পর্যাপ্ত তথ্য পাওয়া যায়নি। আরও বিস্তারিত তথ্য বা পরিষ্কার ছবি দিন।"

            "hi" ->
                "पर्याप्त जानकारी नहीं मिली। कृपया अधिक जानकारी या स्पष्ट तस्वीर दें।"

            "ta" ->
                "போதுமான தகவல் கிடைக்கவில்லை. மேலும் தகவல் அல்லது தெளிவான படத்தை வழங்கவும்।"

            "pa" ->
                "ਲੋੜੀਂਦੀ ਜਾਣਕਾਰੀ ਨਹੀਂ ਮਿਲੀ। ਕਿਰਪਾ ਕਰਕੇ ਹੋਰ ਜਾਣਕਾਰੀ ਜਾਂ ਸਾਫ਼ ਤਸਵੀਰ ਦਿਓ।"

            "te" ->
                "తగినంత సమాచారం అందలేదు. దయచేసి మరింత సమాచారం లేదా స్పష్టమైన చిత్రాన్ని అందించండి।"

            "en" ->
                "Not enough information was available. Please provide more details or a clearer image."

            else ->
                "Not enough information was available. Please provide more details or a clearer image."
        }
    }
}