package ai.openkrishi.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val KrishiGreen = Color(0xFF087443)
private val KrishiDeep = Color(0xFF063D2A)
private val KrishiMint = Color(0xFFE6F6EC)
private val KrishiSky = Color(0xFFEAF5FF)
private val KrishiPurple = Color(0xFFF1EAFF)
private val KrishiAmber = Color(0xFFFFF4D8)
private val Ink = Color(0xFF13231B)
private val Muted = Color(0xFF66736B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenKrishiApp() }
    }
}

private enum class AppTab { HOME, HISTORY, PROFILE }
private enum class ImageSource { CAMERA, GALLERY }
private val LANG_OPTIONS = listOf("বাংলা", "हिन्दी", "தமிழ்", "ਪੰਜਾਬੀ", "తెలుగు", "English")

@Composable
fun OpenKrishiApp() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF6FAF7)) {
            val context = LocalContext.current
            var tab by remember { mutableStateOf(AppTab.HOME) }
            var screen by remember { mutableStateOf("home") }
            var language by remember { mutableStateOf("English") }
            var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
            var imageSource by remember { mutableStateOf<ImageSource?>(null) }
            var cameraDenied by remember { mutableStateOf(false) }
            var advisoryResult by remember { mutableStateOf<AdvisoryResult?>(null) }
            var advisoryError by remember { mutableStateOf<String?>(null) }
            var analyzing by remember { mutableStateOf(false) }
            var advisoryQuery by remember { mutableStateOf("") }
            var weather by remember { mutableStateOf<WeatherResult?>(null) }
            var weatherLoading by remember { mutableStateOf(false) }

            val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    selectedImage = context.contentResolverBitmap(uri)
                    imageSource = ImageSource.GALLERY
                    advisoryResult = null
                    advisoryError = null
                    screen = "diagnosis"
                }
            }
            val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                if (bitmap != null) {
                    selectedImage = bitmap
                    imageSource = ImageSource.CAMERA
                    cameraDenied = false
                    advisoryResult = null
                    advisoryError = null
                    screen = "diagnosis"
                }
            }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                cameraDenied = !granted
                if (granted) cameraLauncher.launch(null)
            }
            fun openCamera() {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraLauncher.launch(null)
                else permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            fun analyze() {
                analyzing = true
                advisoryResult = null
                advisoryError = null
                screen = "result"
                Thread {
                    try {
                        val result = OpenKrishiApi.assessImage(selectedImage ?: throw IllegalStateException("No crop image selected."), language, "rice")
                        Handler(Looper.getMainLooper()).post { advisoryResult = result; analyzing = false }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post { advisoryError = e.message ?: "OpenKrishi AI से कनेक्ट नहीं हो सका।"; analyzing = false }
                    }
                }.start()
            }

            fun askAdvisory(query: String) {
                advisoryQuery = query
                analyzing = true
                advisoryResult = null
                advisoryError = null
                screen = "advisory"
                Thread {
                    try {
                        val result = OpenKrishiApi.getAdvisory(query, language, "rice")
                        Handler(Looper.getMainLooper()).post { advisoryResult = result; analyzing = false }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post { advisoryError = e.message ?: "OpenKrishi AI is unavailable."; analyzing = false }
                    }
                }.start()
            }
            fun loadWeather() {
                weatherLoading = true
                screen = "weather"
                Thread {
                    try {
                        val result = OpenKrishiApi.getWeather(22.5726, 88.3639, language)
                        Handler(Looper.getMainLooper()).post { weather = result; weatherLoading = false }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post { advisoryError = e.message ?: "Weather service is unavailable."; weatherLoading = false }
                    }
                }.start()
            }

            when (screen) {
                "diagnosis" -> DiagnosisScreen(language, selectedImage, imageSource, cameraDenied, { screen = "home" }, { galleryLauncher.launch("image/*") }, ::openCamera, {
                    selectedImage = null; imageSource = null; advisoryResult = null; advisoryError = null
                }, ::analyze, { screen = "voice" })
                "result" -> ResultScreen(selectedImage, advisoryResult, advisoryError, analyzing, { screen = "diagnosis" }, ::analyze, { screen = "voice" })
                "advisory" -> if (advisoryResult == null && !analyzing) AdvisoryInputScreen(language, advisoryQuery, { advisoryQuery = it }, ::askAdvisory) { screen = "home" } else ResultScreen(null, advisoryResult, advisoryError, analyzing, { screen = "home" }, { askAdvisory(advisoryQuery) }, { screen = "voice" })
                "weather" -> WeatherScreen(language, weather, weatherLoading, ::loadWeather) { screen = "home" }
                "voice" -> VoiceScreen(language) { screen = "home" }
                else -> Scaffold(
                    containerColor = Color(0xFFF6FAF7),
                    bottomBar = {
                        NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
                            NavigationBarItem(tab == AppTab.HOME, { tab = AppTab.HOME }, icon = { Text("⌂", fontSize = 24.sp) }, label = { Text("Home") })
                            NavigationBarItem(tab == AppTab.HISTORY, { tab = AppTab.HISTORY }, icon = { Text("◷", fontSize = 22.sp) }, label = { Text("History") })
                            NavigationBarItem(tab == AppTab.PROFILE, { tab = AppTab.PROFILE }, icon = { Text("○", fontSize = 24.sp) }, label = { Text("Profile") })
                        }
                    }
                ) { padding ->
                    when (tab) {
                        AppTab.HOME -> HomeScreen(Modifier.padding(padding), language, { language = it }, { screen = "diagnosis" }, { screen = "voice" }, { screen = "weather"; loadWeather() }, { screen = "advisory" })
                        AppTab.HISTORY -> HistoryScreen(Modifier.padding(padding))
                        AppTab.PROFILE -> ProfileScreen(Modifier.padding(padding), language, { language = it })
                    }
                }
            }
        }
    }
}

private fun Context.contentResolverBitmap(uri: android.net.Uri): Bitmap? = runCatching { contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()

@Composable
private fun HomeScreen(modifier: Modifier, language: String, onLanguage: (String) -> Unit, onDiagnosis: () -> Unit, onVoice: () -> Unit, onWeather: () -> Unit, onAdvice: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(305.dp).background(Brush.verticalGradient(listOf(KrishiDeep, Color(0xFF0B6B45), Color(0xFF65B76B))))) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.15f)), Alignment.Center) { Text("🌿", fontSize = 24.sp) }
                    Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("OpenKrishi AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Smart farming • Better tomorrow", color = Color.White.copy(.78f), fontSize = 12.sp) }
                    Text(language, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp)); Text(if (language == "English") "Hello, Farmer!" else "নমস্কার, কৃষক বন্ধু!",  color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(listOf("বাংলা","हिन्दी","தமிழ்","ਪੰਜਾਬੀ","తెలుగు","English")) { label -> FilterChip(selected = language == label, onClick = { onLanguage(label) }, label = { Text(label, fontSize = 10.sp) }) } }
                Spacer(Modifier.height(7.dp)); Text("আপনার ফসলের সুস্থতা আমাদের লক্ষ্য।\nছবি তুলুন বা কথা বলুন — AI সাহায্য করবে।", color = Color.White.copy(.88f), fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(18.dp)); Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White.copy(.97f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text("🌤️", fontSize = 35.sp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("28°C", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Partly cloudy", color = Muted, fontSize = 12.sp) }; Column(horizontalAlignment = Alignment.End) { Text("⌖ Kolkata", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(if (language == "English") "82% humidity" else "82% আর্দ্রতা",  color = Muted, fontSize = 11.sp) } }
                }
            }
        }
        Column(Modifier.padding(18.dp)) {
            Text(if (language == "English") "Quick help for you" else "আপনার জন্য দ্রুত সাহায্য",  color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { FeatureCard(Modifier.weight(1f), "⌾", if (language == "English") "Crop diagnosis" else "রোগ শনাক্তকরণ", if (language == "English") "Take a photo to find the problem" else "ছবি তুলে সমস্যা জানুন", KrishiMint, KrishiGreen, onDiagnosis); FeatureCard(Modifier.weight(1f), "♩", if (language == "English") "Voice advisory" else "ভয়েস পরামর্শ", if (language == "English") "Speak and hear the answer" else "কথা বলুন, উত্তর শুনুন", KrishiPurple, Color(0xFF7046C8), onVoice) }
            Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { FeatureCard(Modifier.weight(1f), "☁", if (language == "English") "Weather update" else "আবহাওয়া আপডেট", if (language == "English") "Rain and alerts" else "বৃষ্টি ও সতর্কতা", KrishiSky, Color(0xFF2574C8), onWeather); FeatureCard(Modifier.weight(1f), "✦", if (language == "English") "Crop advisory" else "ফসলের পরামর্শ", if (language == "English") "Practical farming guidance" else "চাষের সঠিক গাইড", KrishiAmber, Color(0xFFC48200), onAdvice) }
            Spacer(Modifier.height(22.dp)); Text(if (language == "English") "Popular crops" else "জনপ্রিয় ফসল",  color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(listOf("🌾\nধান", "🌿\nপাট", "🥜\nবাদাম", "🥬\nসবজি", "🌸\nফুল")) { crop -> Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.width(78.dp)) { Text(crop, Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center, fontSize = 14.sp, lineHeight = 22.sp) } } }
            Spacer(Modifier.height(22.dp)); Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(KrishiMint), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text("✓", color = KrishiGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp)); Column { Text(if (language == "English") "In the farmer's language, by the farmer's side" else "কৃষকের ভাষায়, কৃষকের পাশে",  color = KrishiDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(if (language == "English") "AI Vision • Voice • Weather • 6 languages" else "AI Vision • Voice • Weather • ৬টি ভাষা",  color = Muted, fontSize = 11.sp) } } }
        }
    }
}

@Composable
private fun FeatureCard(modifier: Modifier, icon: String, title: String, subtitle: String, tint: Color, iconTint: Color, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) { Column(Modifier.padding(14.dp)) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(tint), Alignment.Center) { Text(icon, color = iconTint, fontSize = 22.sp) }; Spacer(Modifier.height(12.dp)); Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(Modifier.height(4.dp)); Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp) } }
}

@Composable
private fun DiagnosisScreen(language: String, selectedImage: Bitmap?, imageSource: ImageSource?, cameraDenied: Boolean, onBack: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit, onClear: () -> Unit, onAnalyze: () -> Unit, onVoice: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("‹", fontSize = 36.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack)); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text("AI Crop Diagnosis", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink); Text(if (language == "English") "Identify crop problems from a photo" else "ফসলের ছবি দিয়ে সমস্যা শনাক্ত করুন",  fontSize = 12.sp, color = Muted) }; Text(language, fontSize = 12.sp, color = KrishiGreen, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(18.dp))
        if (selectedImage == null) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(78.dp).clip(RoundedCornerShape(22.dp)).background(KrishiMint), Alignment.Center) { Text("🌱", fontSize = 42.sp) }; Spacer(Modifier.height(12.dp)); Text(if (language == "English") "Add a clear crop photo" else "ফসলের একটি পরিষ্কার ছবি দিন",  fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(5.dp)); Text(if (language == "English") "Capture the leaf, stem, or affected area up close" else "পাতা, কান্ড বা আক্রান্ত অংশটি কাছ থেকে তুলুন",  fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center) } }
            Spacer(Modifier.height(14.dp)); Text(if (language == "English") "Choose an image" else "ছবি বাছাই করুন",  fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ImageSourceCard(Modifier.weight(1f), "📷", if (language == "English") "Camera" else "ক্যামেরা", if (language == "English") "Take a photo" else "সরাসরি ছবি", KrishiMint, onCamera); ImageSourceCard(Modifier.weight(1f), "🖼️", if (language == "English") "Gallery" else "গ্যালারি", if (language == "English") "Choose from phone" else "ফোনের ছবি", KrishiSky, onGallery) }
            if (cameraDenied) { Spacer(Modifier.height(10.dp)); Text("ক্যামেরা অনুমতি দেওয়া হয়নি। গ্যালারি থেকেও ছবি বেছে নিতে পারেন।", color = Color(0xFF9A5B00), fontSize = 12.sp) }
        } else {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Image(selectedImage.asImageBitmap(), "Selected crop image", Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop); Spacer(Modifier.height(10.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("ছবি প্রস্তুত", color = KrishiGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(sourceLabel(imageSource), color = Muted, fontSize = 11.sp) }; Text("পরিবর্তন করুন", color = KrishiGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onClear)) } } }
            Spacer(Modifier.height(14.dp)); Text("অন্য ছবি ব্যবহার করবেন?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(9.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = onCamera, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp)) { Text("📷 ক্যামেরা", fontSize = 11.sp) }; OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp)) { Text("🖼️ গ্যালারি", fontSize = 11.sp) } }
            Spacer(Modifier.height(20.dp)); Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(KrishiGreen)) { Text(if (language == "English") "Analyze with AI" else "AI দিয়ে বিশ্লেষণ করুন",  fontSize = 16.sp, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onVoice, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text(if (language == "English") "🎙️ Get voice advisory" else "🎙️ ভয়েসে পরামর্শ নিন") }
        }
        Spacer(Modifier.height(20.dp)); Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(KrishiMint), modifier = Modifier.fillMaxWidth()) { Text(if (language == "English") "Tip: Take a close, well-lit photo of the affected area for clearer AI analysis." else "টিপস: ভালো আলোতে পাতার আক্রান্ত অংশটি কাছ থেকে তুললে AI বিশ্লেষণ আরও পরিষ্কার হতে পারে।", color = KrishiDeep, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(14.dp)) }
    }
}

@Composable
private fun ImageSourceCard(modifier: Modifier, icon: String, title: String, subtitle: String, tint: Color, onClick: () -> Unit) { Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) { Column(Modifier.padding(vertical = 18.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(tint), Alignment.Center) { Text(icon, fontSize = 26.sp) }; Spacer(Modifier.height(9.dp)); Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(3.dp)); Text(subtitle, fontSize = 11.sp, color = Muted) } } }
private fun sourceLabel(source: ImageSource?): String = when (source) { ImageSource.CAMERA -> "ক্যামেরা থেকে নেওয়া ছবি"; ImageSource.GALLERY -> "গ্যালারি থেকে নির্বাচিত ছবি"; null -> "ছবি" }

@Composable
private fun ResultScreen(selectedImage: Bitmap?, result: AdvisoryResult?, error: String?, analyzing: Boolean, onBack: () -> Unit, onRetry: () -> Unit, onVoice: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("‹", fontSize = 36.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack)); Text("বিশ্লেষণের ফলাফল", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(14.dp))
        if (selectedImage != null) { Image(selectedImage.asImageBitmap(), "Crop", Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop); Spacer(Modifier.height(14.dp)) }
        when {
            analyzing -> { Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = KrishiGreen); Spacer(Modifier.height(14.dp)); Text("OpenKrishi AI থেকে পরামর্শ আনা হচ্ছে…", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text("আপনার নির্বাচিত ভাষায় উত্তর প্রস্তুত হচ্ছে।", color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp) } } }
            error != null -> { ResultCard("⚠️", "সংযোগ সমস্যা", "পরামর্শ আনা যায়নি: $error"); Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(KrishiGreen)) { Text("আবার চেষ্টা করুন") } }
            result != null -> { ResultCard("🌾", "ফসল", "ধান"); if (result.observations.isNotEmpty()) ResultCard("👁️", "ছবিতে দেখা লক্ষণ", result.observations.joinToString("\n• ", prefix = "• ")); if (result.possibleCauses.isNotEmpty()) ResultCard("🔎", "সম্ভাব্য কারণ", result.possibleCauses.joinToString("\n• ", prefix = "• ")); ResultCard("💡", "AI পরামর্শ", result.answer); if (result.recommendations.isNotEmpty()) ResultCard("✓", "নিরাপদ পরবর্তী পদক্ষেপ", result.recommendations.joinToString("\n• ", prefix = "• ")); ResultCard("◉", "Confidence", result.confidence); ResultCard("✓", "Safety", result.safety) }
            else -> ResultCard("ℹ️", "অপেক্ষা করুন", "বিশ্লেষণ শুরু করা হয়নি।")
        }
        Spacer(Modifier.height(10.dp)); Button(onClick = onVoice, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(KrishiGreen)) { Text("🔊 পরামর্শ শুনুন", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp)); Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(KrishiMint), modifier = Modifier.fillMaxWidth()) { Text("ছবিটি AI Vision-এ পাঠিয়ে নির্বাচিত ভাষায় ফলাফল তৈরি করা হয়েছে।", color = KrishiDeep, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(13.dp)) }
    }
}

@Composable
private fun ResultCard(icon: String, title: String, body: String) { Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) { Text(icon, fontSize = 22.sp); Spacer(Modifier.width(10.dp)); Column { Text(title, color = KrishiGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(body, color = Ink, fontSize = 14.sp, lineHeight = 20.sp) } } } }

@Composable
private fun VoiceScreen(language: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val tts = remember { TextToSpeech(context) { } }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        listening = false
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (text.isNotBlank()) {
            transcript = text
            loading = true
            error = null
            Thread {
                try {
                    val r = OpenKrishiApi.getAdvisory(text, language, "rice")
                    Handler(Looper.getMainLooper()).post {
                        answer = r.answer
                        loading = false
                        val locale = when(language) {
                            "বাংলা" -> Locale("bn","IN")
                            "हिन्दी" -> Locale("hi","IN")
                            "தமிழ்" -> Locale("ta","IN")
                            "ਪੰਜਾਬੀ" -> Locale("pa","IN")
                            "తెలుగు" -> Locale("te","IN")
                            else -> Locale.US
                        }
                        tts.language = locale
                        tts.speak(r.answer, TextToSpeech.QUEUE_FLUSH, null, "openkrishi-advisory")
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { error = e.message ?: "AI service is unavailable."; loading = false }
                }
            }.start()
        }
    }
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error = "Speech recognition is not available on this device."
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, when(language) {
                "বাংলা" -> "bn-IN"; "हिन्दी" -> "hi-IN"; "தமிழ்" -> "ta-IN"
                "ਪੰਜਾਬੀ" -> "pa-IN"; "తెలుగు" -> "te-IN"; else -> "en-IN"
            })
            putExtra(RecognizerIntent.EXTRA_PROMPT, "OpenKrishi AI")
        }
        listening = true
        launcher.launch(intent)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(if (language == "English") "Voice advisory" else "ভয়েস পরামর্শ", language, onBack)
        Spacer(Modifier.height(20.dp))
        Card(shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(KrishiPurple), modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(25.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Text("🎙️", fontSize=55.sp)
                Spacer(Modifier.height(10.dp))
                Text(if(listening) { if (language == "English") "Listening…" else "শুনছি…" } else { if (language == "English") "Describe your crop problem" else "আপনার ফসলের সমস্যা বলুন" }, fontSize=20.sp, fontWeight=FontWeight.Bold, color=Ink)
                Spacer(Modifier.height(14.dp))
                Button(onClick=::startListening, enabled=!listening && !loading, colors=ButtonDefaults.buttonColors(KrishiGreen)) {
                    Text(if(listening) { if (language == "English") "● Listening" else "● শুনছি" } else { if (language == "English") "🎙️ Start speaking" else "🎙️ কথা বলা শুরু করুন" })
                }
            }
        }
        if (loading) { Spacer(Modifier.height(15.dp)); CircularProgressIndicator(color=KrishiGreen) }
        if (transcript.isNotBlank()) { Spacer(Modifier.height(15.dp)); ResultCard("📝","আপনি বলেছেন",transcript) }
        if (answer.isNotBlank()) { ResultCard("💡","AI পরামর্শ",answer); OutlinedButton(onClick={ tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "openkrishi-advisory") }, modifier=Modifier.fillMaxWidth()){ Text("🔊 আবার শুনুন") } }
        error?.let { Spacer(Modifier.height(10.dp)); Text(it,color=Color(0xFF9A5B00),fontSize=12.sp) }
        Spacer(Modifier.height(12.dp)); Text(if (language == "English") "Voice input uses your selected language, and AI advice is returned in the same language." else "ভয়েস ইনপুট আপনার নির্বাচিত ভাষায় নেওয়া হবে এবং AI পরামর্শ একই ভাষায় দেওয়া হবে।",color=Muted,fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
    }
}

@Composable
private fun WeatherScreen(language: String, weather: WeatherResult?, loading: Boolean, onRefresh: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(if(language=="English") "Weather" else "আবহাওয়া", language, onBack)
        Spacer(Modifier.height(15.dp))
        if (loading) { CircularProgressIndicator(color=KrishiGreen); Spacer(Modifier.height(12.dp)) }
        weather?.let { w ->
            ResultCard("🌤️", w.location, "${w.temperature} • ${w.description}\n${w.humidity}")
            ResultCard("⚠️", if(language=="English") "Farm alert" else "কৃষি সতর্কতা", w.alert)
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick=onRefresh, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(KrishiGreen)) { Text(if(language=="English") "Refresh weather" else "আবহাওয়া আপডেট করুন") }
    }
}

@Composable
private fun AdvisoryInputScreen(language:String, query:String, onQuery:(String)->Unit, onAsk:(String)->Unit, onBack:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(if(language=="English") "Crop advisory" else "ফসলের পরামর্শ", language, onBack)
        Spacer(Modifier.height(18.dp))
        Text(if(language=="English") "Describe your crop problem" else "আপনার ফসলের সমস্যাটি লিখুন", color=Ink, fontSize=18.sp, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value=query, onValueChange=onQuery, modifier=Modifier.fillMaxWidth().height(150.dp), placeholder={Text(if(language=="English") "Example: rice leaves are turning yellow" else "উদাহরণ: ধানের পাতা হলুদ হয়ে যাচ্ছে")})
        Spacer(Modifier.height(12.dp))
        Button(onClick={onAsk(query)}, enabled=query.isNotBlank(), modifier=Modifier.fillMaxWidth().height(52.dp), colors=ButtonDefaults.buttonColors(KrishiGreen)) { Text(if(language=="English") "Get AI advisory" else "AI পরামর্শ নিন", fontWeight=FontWeight.Bold) }
    }
}

@Composable
private fun HistoryScreen(modifier: Modifier) { Column(modifier.fillMaxSize().padding(18.dp)) { Text("History", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(8.dp)); Text("Your previous AI advice will appear here.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(18.dp)); Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) { Text("No saved analyses yet.", color = Muted, modifier = Modifier.padding(18.dp)) } } }

@Composable
private fun ProfileScreen(modifier: Modifier, language: String, onLanguage: (String) -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("Profile", fontSize=24.sp, fontWeight=FontWeight.Bold, color=Ink)
        Spacer(Modifier.height(18.dp))
        Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(Color.White), modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("🌾 Farmer profile", fontSize=18.sp, fontWeight=FontWeight.Bold, color=Ink)
                Spacer(Modifier.height(8.dp)); Text("নির্বাচিত ভাষা: $language", color=Muted, fontSize=13.sp)
                Spacer(Modifier.height(12.dp)); Text("ভাষা নির্বাচন করুন", color=Ink, fontWeight=FontWeight.Bold)
                LANG_OPTIONS.forEach { label ->
                    Row(Modifier.fillMaxWidth().clickable { onLanguage(label) }.padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=language==label, onClick={onLanguage(label)}); Text(label, color=Ink)
                    }
                }
            }
        }
    }
}


@Composable
private fun TopBar(title: String, language: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("‹", fontSize = 36.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(language, color = KrishiGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}