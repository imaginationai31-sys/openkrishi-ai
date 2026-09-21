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
        val body = JSONObject().apply {
            put("query", query)
            put("language", languageCode(language))
            put("crop_category", cropCategory)
            if (!cropName.isNullOrBlank()) put("crop_name", cropName)
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
            val code = connection.responseCode
            val text = readResponse(connection)
            if (code !in 200..299) throw IllegalStateException("API request failed ($code)")
            val json = JSONObject(text)
            fun list(key: String): List<String> {
                val array = json.optJSONArray(key) ?: return emptyList()
                return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
            }
            AdvisoryResult(
                answer = json.optString("answer", "No advisory was returned."),
                confidence = json.optString("confidence", "unknown"),
                safety = json.optString("safety", "unknown"),
                observations = list("observations"),
                possibleCauses = list("possible_causes"),
                recommendations = list("recommendations"),
                cropCategory = json.optString("crop_category", cropCategory)
            )
        } finally {
            connection.disconnect()
        }
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
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
        val imageBytes = output.toByteArray()
        val boundary = "----OpenKrishiBoundary" + System.currentTimeMillis()
        val connection = (URL("$OPENKRISHI_API_BASE/api/v1/vision/assess").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { out ->
                fun field(name: String, value: String) {
                    out.write("--$boundary\r\n".toByteArray())
                    out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.write("\r\n".toByteArray())
                }
                field("crop_category", cropCategory)
                field("language", languageCode(language))
                if (!growthStage.isNullOrBlank()) field("growth_stage", growthStage)
                if (!cropName.isNullOrBlank()) field("crop_name", cropName)
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"crop.jpg\"\r\n".toByteArray())
                out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                out.write(imageBytes)
                out.write("\r\n".toByteArray())
                out.write("--$boundary--\r\n".toByteArray())
            }
            val code = connection.responseCode
            val text = readResponse(connection)
            if (code !in 200..299) throw IllegalStateException("Vision API request failed ($code)")
            val json = JSONObject(text)
            val vision = json.optJSONObject("vision") ?: JSONObject()
            val advisory = json.optJSONObject("advisory") ?: JSONObject()
            fun list(obj: JSONObject, key: String): List<String> {
                val array = obj.optJSONArray(key) ?: return emptyList()
                return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
            }
            AdvisoryResult(
                answer = advisory.optString("answer", "No advisory was returned."),
                confidence = vision.optString("confidence", advisory.optString("confidence", "unknown")),
                safety = vision.optString("safety", advisory.optString("safety", "unknown")),
                observations = list(vision, "observations"),
                possibleCauses = list(vision, "possible_causes"),
                recommendations = list(vision, "recommendations"),
                cropCategory = json.optString("crop_category", cropCategory)
            )
        } finally {
            connection.disconnect()
        }
    }
}