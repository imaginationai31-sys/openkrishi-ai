package ai.openkrishi.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val KrishiGreen = Color(0xFF087443)
val KrishiDeep = Color(0xFF063D2A)
val KrishiMint = Color(0xFFE6F6EC)
val KrishiSky = Color(0xFFEAF5FF)
val KrishiPurple = Color(0xFFF1EAFF)
val KrishiAmber = Color(0xFFFFF4D8)
val Ink = Color(0xFF13231B)
val Muted = Color(0xFF66736B)

enum class ImageSource { CAMERA, GALLERY }

fun tx(language: String, en: String, bn: String, hi: String, ta: String, pa: String, te: String): String = when (language) {
    "বাংলা" -> bn
    "हिन्दी" -> hi
    "தமிழ்" -> ta
    "ਪੰਜਾਬੀ" -> pa
    "తెలుగు" -> te
    else -> en
}

fun cropLabel(crop: String, language: String): String = when (crop) {
    "rice" -> tx(language, "Rice", "ধান", "धान", "நெல்", "ਚੌਲ", "వరి")
    "peanut" -> tx(language, "Peanut", "বাদাম", "मूंगफली", "நிலக்கடலை", "ਮੂੰਗਫਲੀ", "వేరుశెనగ")
    "vegetables" -> tx(language, "Vegetables", "সবজি", "सब्ज़ियाँ", "காய்கறிகள்", "ਸਬਜ਼ੀਆਂ", "కూరగాయలు")
    "flowers" -> tx(language, "Flowers", "ফুল", "फूल", "மலர்கள்", "ਫੁੱਲ", "పూలు")
    else -> crop
}

@Composable
fun TopBar(title: String, language: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("‹", fontSize = 36.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(language, color = KrishiGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

data class HistoryEntry(
    val crop: String,
    val cropName: String,
    val type: String,
    val summary: String,
    val timestamp: String
)

private const val HISTORY_PREF = "openkrishi_history_v2"

fun loadHistory(context: Context): List<HistoryEntry> = runCatching {
    val raw = context.getSharedPreferences(HISTORY_PREF, Context.MODE_PRIVATE).getString("items", "[]") ?: "[]"
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { i ->
        array.optJSONObject(i)?.let { o ->
            HistoryEntry(o.optString("crop"), o.optString("cropName"), o.optString("type"), o.optString("summary"), o.optString("timestamp"))
        }
    }
}.getOrDefault(emptyList())

fun saveHistory(context: Context, entries: List<HistoryEntry>) {
    val array = JSONArray()
    entries.take(30).forEach { e ->
        array.put(JSONObject().apply {
            put("crop", e.crop); put("cropName", e.cropName); put("type", e.type)
            put("summary", e.summary); put("timestamp", e.timestamp)
        })
    }
    context.getSharedPreferences(HISTORY_PREF, Context.MODE_PRIVATE).edit().putString("items", array.toString()).apply()
}

fun addHistory(context: Context, current: List<HistoryEntry>, crop: String, cropName: String, type: String, summary: String): List<HistoryEntry> {
    val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    val updated = listOf(HistoryEntry(crop, cropName, type, summary.take(500), stamp)) + current
    val result = updated.take(30)
    saveHistory(context, result)
    return result
}


fun ui(language: String, key: String): String = when (key) {
    "home" -> tx(language, "Home", "হোম", "होम", "முகப்பு", "ਹੋਮ", "హోమ్")
    "history" -> tx(language, "History", "ইতিহাস", "इतिहास", "வரலாறு", "ਇਤਿਹਾਸ", "చరిత్ర")
    "profile" -> tx(language, "Profile", "প্রোফাইল", "प्रोफ़ाइल", "சுயவிவரம்", "ਪ੍ਰੋਫਾਈਲ", "ప్రొఫైల్")
    else -> key
}
