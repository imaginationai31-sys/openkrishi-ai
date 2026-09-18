package ai.openkrishi.app

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

private const val OPENKRISHI_API_BASE = "https://openkrishi-ai-api.onrender.com"

internal data class AdvisoryResult(
    val answer: String,
    val confidence: String,
    val safety: String,
    val observations: List<String> = emptyList(),
    val possibleCauses: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val cropCategory: String? = null
)

private fun languageCode(language: String): String = when (language) {
    "বাংলা" -> "bn"
    "हिन्दी" -> "hi"
    "தமிழ்" -> "ta"
    "ਪੰਜਾਬੀ" -> "pa"
    "తెలుగు" -> "te"
    else -> "en"
}

internal object OpenKrishiApi {
    fun getAdvisory(
        query: String,
        language: String,
        cropCategory: String = "rice"
    ): AdvisoryResult {
        val languageCode = when (language) {
            "বাংলা" -> "bn"
            "हिन्दी" -> "hi"
            "தமிழ்" -> "ta"
            "ਪੰਜਾਬੀ" -> "pa"
            "తెలుగు" -> "te"
            else -> "en"
        }

        val body = JSONObject().apply {
            put("query", query)
            put("language", languageCode)
            put("crop_category", cropCategory)
        }.toString()

        val connection = (URL("$OPENKRISHI_API_BASE/api/v1/advisory").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                throw IllegalStateException("API request failed ($responseCode)")
            }

            val json = JSONObject(responseText)
            AdvisoryResult(
                answer = json.optString("answer", "কোনো পরামর্শ পাওয়া যায়নি।"),
                confidence = json.optString("confidence", "unknown"),
                safety = json.optString("safety", "unknown")
            )
        } finally {
            connection.disconnect()
        }
    }
}
