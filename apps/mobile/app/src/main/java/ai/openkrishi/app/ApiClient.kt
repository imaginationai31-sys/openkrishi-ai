package ai.openkrishi.app

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
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


internal data class WeatherResult(
    val temperature: String,
    val description: String,
    val humidity: String,
    val location: String,
    val alert: String
)

private fun languageCode(language: String): String = when (language) {
    "বাংলা" -> "bn"
    "हिन्दी" -> "hi"
    "தமிழ்" -> "ta"
    "ਪੰਜਾਬੀ" -> "pa"
    "తెలుగు" -> "te"
    else -> "en"
}

private fun readResponse(connection: HttpURLConnection): String {
    val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
    return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
}

internal object OpenKrishiApi {
    fun getAdvisory(query: String, language: String, cropCategory: String = "rice", cropName: String? = null): AdvisoryResult {
        return FirebaseAiManager.advisory(query, language, cropCategory, cropName)
    }

    fun getWeather(latitude: Double, longitude: Double, language: String): WeatherResult {
        val url = URL("$OPENKRISHI_API_BASE/api/v1/weather?latitude=$latitude&longitude=$longitude&language=${languageCode(language)}&forecast_days=1")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            val text = readResponse(connection)
            if (code !in 200..299) throw IllegalStateException("Weather API request failed ($code)")
            val json = JSONObject(text)
            val current = json.optJSONObject("current") ?: JSONObject()
            val location = json.optJSONObject("location") ?: JSONObject()
            WeatherResult(
                temperature = current.optString("temperature_2m", "--") + "°C",
                description = current.optString("weather_description", "Weather unavailable"),
                humidity = current.optString("relative_humidity_2m", "--") + "%",
                location = String.format(Locale.US, "%.3f, %.3f", location.optDouble("latitude", latitude), location.optDouble("longitude", longitude)),
                alert = json.optString("farm_alert", "No major farm alert.")
            )
        } finally { connection.disconnect() }
    }

    fun assessImage(bitmap: Bitmap, language: String, cropCategory: String = "rice", growthStage: String? = null, cropName: String? = null): AdvisoryResult {
        return FirebaseAiManager.vision(bitmap, language, cropCategory, growthStage, cropName)
    }

}