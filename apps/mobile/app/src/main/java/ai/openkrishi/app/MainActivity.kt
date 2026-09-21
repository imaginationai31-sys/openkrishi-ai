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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
            var selectedCrop by remember { mutableStateOf("rice") }
            var selectedCropName by remember { mutableStateOf("") }

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
                        val result = OpenKrishiApi.assessImage(selectedImage ?: throw IllegalStateException("No crop image selected."), language, selectedCrop, cropName = selectedCropName)
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
                        val result = OpenKrishiApi.getAdvisory(query, language, selectedCrop, selectedCropName)
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
                "diagnosis" -> ProductionDiagnosisScreen(language, selectedCrop, { selectedCrop = it; selectedCropName = "" }, selectedCropName, { selectedCropName = it }, selectedImage, imageSource, cameraDenied, { screen = "home" }, { galleryLauncher.launch("image/*") }, ::openCamera, {
                    selectedImage = null; imageSource = null; advisoryResult = null; advisoryError = null
                }, ::analyze, { screen = "voice" })
                "result" -> ProductionResultScreen(selectedImage, selectedCrop, selectedCropName, language, advisoryResult, advisoryError, analyzing, { screen = "diagnosis" }, ::analyze, { screen = "voice" })
                "advisory" -> if (advisoryResult == null && !analyzing) AdvisoryInputScreen(language, selectedCrop, { selectedCrop = it; selectedCropName = "" }, selectedCropName, { selectedCropName = it }, advisoryQuery, { advisoryQuery = it }, ::askAdvisory) { screen = "home" } else ResultScreen(null, selectedCrop, selectedCropName, language, advisoryResult, advisoryError, analyzing, { screen = "home" }, { askAdvisory(advisoryQuery) }, { screen = "voice" })
                "weather" -> WeatherScreen(language, weather, weatherLoading, ::loadWeather) { screen = "home" }
                "voice" -> VoiceScreen(language, selectedCrop, { selectedCrop = it; selectedCropName = "" }, selectedCropName, { selectedCropName = it }) { screen = "home" }
                else -> Scaffold(
                    containerColor = Color(0xFFF6FAF7),
                    bottomBar = {
                        NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
                            NavigationBarItem(tab == AppTab.HOME, { tab = AppTab.HOME }, icon = { Text("⌂", fontSize = 24.sp) }, label = { Text(ui(language, "home")) })
                            NavigationBarItem(tab == AppTab.HISTORY, { tab = AppTab.HISTORY }, icon = { Text("◷", fontSize = 22.sp) }, label = { Text(ui(language, "history")) })
                            NavigationBarItem(tab == AppTab.PROFILE, { tab = AppTab.PROFILE }, icon = { Text("○", fontSize = 24.sp) }, label = { Text(ui(language, "profile")) })
                        }
                    }
                ) { padding ->
                    when (tab) {
                        AppTab.HOME -> HomeDashboard(Modifier.padding(padding), language, { language = it }, { screen = "diagnosis" }, { screen = "voice" }, { screen = "weather"; loadWeather() }, { screen = "advisory" })
                        AppTab.HISTORY -> HistoryScreen(Modifier.padding(padding), language)
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
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF7FBF8))) {
        // Light, landscape-inspired header matching the web reference.
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFF3FAF6), Color(0xFFDFF2E3))
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp), Alignment.Center) { Text("🌱", fontSize = 42.sp) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "OpenKrishi AI",
                        color = Color(0xFF103F31),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp
                    )
                    Text(
                        "Smart Advice • Healthy Crops • Prosperous Farmers",
                        color = Color(0xFF5B7068),
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        "🌐 $language⌄",
                        color = Color(0xFF187A66),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = .82f))
                    .padding(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📍", fontSize = 27.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kolkata, West Bengal", color = Color(0xFF177A68), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("22.6011° N, 88.3176° E", color = Muted, fontSize = 10.sp)
                }
                Text("🌤️", fontSize = 29.sp)
                Spacer(Modifier.width(7.dp))
                Column {
                    Text("28°C", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(
                        tx(language, "Partly Cloudy", "আংশিক মেঘলা", "आंशिक बादल", "ஓரளவு மேகமூட்டம்", "ਅੰਸ਼ਿਕ ਬੱਦਲ", "పాక్షికంగా మేఘావృతం"),
                        color = Muted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                ui(language, "quick_help"),
                color = Color(0xFF0C6348),
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FeatureCard(Modifier.weight(1f), FeatureKind.DIAGNOSIS, ui(language, "diagnosis"),
                    if (language == "English") "Take a photo to find the problem" else "ছবি তুলে সমস্যা জানুন", onDiagnosis)
                FeatureCard(Modifier.weight(1f), FeatureKind.VOICE, ui(language, "voice"),
                    if (language == "English") "Speak and hear the answer" else "কথা বলুন, উত্তর শুনুন", onVoice)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FeatureCard(Modifier.weight(1f), FeatureKind.WEATHER, ui(language, "weather"),
                    if (language == "English") "Rain and alerts" else "বৃষ্টি ও সতর্কতা", onWeather)
                FeatureCard(Modifier.weight(1f), FeatureKind.ADVISORY, ui(language, "advisory"),
                    if (language == "English") "Practical farming guidance" else "চাষের সঠিক গাইড", onAdvice)
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ui(language, "popular"), color = Color(0xFF0C6348), fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("View all crops  →", color = Color(0xFF13745B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(
                    listOf(
                        Triple("🌾", cropLabel("rice", language), Color(0xFFE5F6E9)),
                        Triple("🥜", cropLabel("peanut", language), Color(0xFFFFF0CE)),
                        Triple("🥬", cropLabel("vegetables", language), Color(0xFFE6F6D2)),
                        Triple("🌸", cropLabel("flowers", language), Color(0xFFF1E1FF))
                    )
                ) { crop ->
                    CropVisualCard(
                        modifier = Modifier.width(150.dp),
                        emoji = crop.first,
                        name = crop.second,
                        background = crop.third
                    )
                }
            }
        }
    }
}

private enum class FeatureKind { DIAGNOSIS, VOICE, WEATHER, ADVISORY }

@Composable
private fun FeatureCard(
    modifier: Modifier,
    kind: FeatureKind,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = when (kind) {
        FeatureKind.DIAGNOSIS -> listOf(Color(0xFF69DC2C), Color(0xFF087344))
        FeatureKind.VOICE -> listOf(Color(0xFFBE62FA), Color(0xFF5430DF))
        FeatureKind.WEATHER -> listOf(Color(0xFF18BDE9), Color(0xFF087CE5))
        FeatureKind.ADVISORY -> listOf(Color(0xFFFFC51B), Color(0xFFFF7415))
    }

    Card(
        modifier = modifier
            .height(226.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors))
                .padding(13.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color.White.copy(alpha = .09f)),
                    Alignment.Center
                ) {
                    FeatureIllustration(kind)
                }
                Spacer(Modifier.height(8.dp))
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Color.White.copy(.96f), fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(.22f)),
                Alignment.Center
            ) { Text("→", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Light) }
        }
    }
}

@Composable
private fun FeatureIllustration(kind: FeatureKind) {
    Canvas(Modifier.fillMaxSize().padding(12.dp)) {
        val w = size.width
        val h = size.height
        when (kind) {
            FeatureKind.DIAGNOSIS -> {
                // Camera body + lens + leaf with a disease spot, echoing the reference artwork.
                drawRoundRect(color = Color(0xFF1E293B), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f,18f), topLeft = androidx.compose.ui.geometry.Offset(w*.17f,h*.25f), size = androidx.compose.ui.geometry.Size(w*.66f,h*.48f))
                drawRoundRect(color = Color(0xFF0F172A), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f,10f), topLeft = androidx.compose.ui.geometry.Offset(w*.39f,h*.18f), size = androidx.compose.ui.geometry.Size(w*.22f,h*.12f))
                drawCircle(Color(0xFF38BDF8), w*.13f, androidx.compose.ui.geometry.Offset(w*.50f,h*.49f))
                drawCircle(Color(0xFF082F49), w*.075f, androidx.compose.ui.geometry.Offset(w*.50f,h*.49f))
                drawCircle(Color(0xFF7DD3FC), w*.025f, androidx.compose.ui.geometry.Offset(w*.46f,h*.45f))
                drawOval(Color(0xFF4ADE80), androidx.compose.ui.geometry.Offset(w*.68f,h*.30f), androidx.compose.ui.geometry.Size(w*.20f,h*.34f))
                drawCircle(Color(0xFFEAB308), w*.025f, androidx.compose.ui.geometry.Offset(w*.74f,h*.40f))
                drawCircle(Color(0xFFEAB308), w*.018f, androidx.compose.ui.geometry.Offset(w*.79f,h*.47f))
            }
            FeatureKind.VOICE -> {
                drawCircle(Color.White.copy(.20f), w*.30f, androidx.compose.ui.geometry.Offset(w*.50f,h*.47f))
                drawRoundRect(color = Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f,28f), topLeft = androidx.compose.ui.geometry.Offset(w*.43f,h*.22f), size = androidx.compose.ui.geometry.Size(w*.14f,h*.47f))
                val stroke = 6f
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(w*.50f,h*.69f), androidx.compose.ui.geometry.Offset(w*.50f,h*.83f), strokeWidth=stroke, cap=StrokeCap.Round)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(w*.39f,h*.84f), androidx.compose.ui.geometry.Offset(w*.61f,h*.84f), strokeWidth=stroke, cap=StrokeCap.Round)
                for (i in 0..3) {
                    val x = w*(.22f + i*.06f)
                    val hh = h*(.15f + i*.04f)
                    drawLine(Color.White.copy(.95f), androidx.compose.ui.geometry.Offset(x,h*.52f-hh/2), androidx.compose.ui.geometry.Offset(x,h*.52f+hh/2), strokeWidth=5f, cap=StrokeCap.Round)
                    val xr = w*(.78f - i*.06f)
                    drawLine(Color.White.copy(.95f), androidx.compose.ui.geometry.Offset(xr,h*.52f-hh/2), androidx.compose.ui.geometry.Offset(xr,h*.52f+hh/2), strokeWidth=5f, cap=StrokeCap.Round)
                }
            }
            FeatureKind.WEATHER -> {
                drawCircle(Color(0xFFFFC928), w*.22f, androidx.compose.ui.geometry.Offset(w*.38f,h*.38f))
                for (i in 0..7) {
                    val a = Math.toRadians((i*45).toDouble())
                    val x1=w*.38f+Math.cos(a).toFloat()*w*.29f; val y1=h*.38f+Math.sin(a).toFloat()*h*.29f
                    val x2=w*.38f+Math.cos(a).toFloat()*w*.38f; val y2=h*.38f+Math.sin(a).toFloat()*h*.38f
                    drawLine(Color(0xFFFFE15B), androidx.compose.ui.geometry.Offset(x1,y1), androidx.compose.ui.geometry.Offset(x2,y2), strokeWidth=5f, cap=StrokeCap.Round)
                }
                drawCircle(Color(0xFFE8F4FF), w*.19f, androidx.compose.ui.geometry.Offset(w*.58f,h*.52f))
                drawCircle(Color(0xFFF3FAFF), w*.22f, androidx.compose.ui.geometry.Offset(w*.72f,h*.54f))
                drawRoundRect(color = Color(0xFFEAF7FF), cornerRadius = androidx.compose.ui.geometry.CornerRadius(30f,30f), topLeft=androidx.compose.ui.geometry.Offset(w*.45f,h*.48f), size=androidx.compose.ui.geometry.Size(w*.40f,h*.22f))
                for (x in listOf(.55f,.68f,.81f)) {
                    drawOval(Color(0xFF33D2FF), androidx.compose.ui.geometry.Offset(w*x,h*.74f), androidx.compose.ui.geometry.Size(w*.045f,h*.13f))
                }
            }
            FeatureKind.ADVISORY -> {
                drawCircle(Color(0xFFFFF4A3), w*.29f, androidx.compose.ui.geometry.Offset(w*.50f,h*.38f))
                drawCircle(Color(0xFFFFFDE7), w*.24f, androidx.compose.ui.geometry.Offset(w*.50f,h*.38f))
                drawRoundRect(color = Color(0xFF374151), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f,8f), topLeft=androidx.compose.ui.geometry.Offset(w*.43f,h*.55f), size=androidx.compose.ui.geometry.Size(w*.14f,h*.20f))
                drawLine(Color(0xFF166534), androidx.compose.ui.geometry.Offset(w*.50f,h*.57f), androidx.compose.ui.geometry.Offset(w*.50f,h*.36f), strokeWidth=8f, cap=StrokeCap.Round)
                drawOval(Color(0xFF39B54A), androidx.compose.ui.geometry.Offset(w*.39f,h*.39f), androidx.compose.ui.geometry.Size(w*.18f,h*.10f))
                drawOval(Color(0xFF2FA43E), androidx.compose.ui.geometry.Offset(w*.51f,h*.34f), androidx.compose.ui.geometry.Size(w*.18f,h*.10f))
                drawArc(Color.White, 200f, 140f, false, androidx.compose.ui.geometry.Offset(w*.10f,h*.65f), androidx.compose.ui.geometry.Size(w*.80f,h*.30f), style=androidx.compose.ui.graphics.drawscope.Stroke(width=7f))
            }
        }
    }
}

@Composable
private fun CropVisualCard(modifier: Modifier, emoji: String, name: String, background: Color) {
    Card(
        modifier = modifier.height(178.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(.35f), background)
                        )
                    ),
                Alignment.Center
            ) {
                Text(emoji, fontSize = 72.sp)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = if (name.contains("Flower", true) || name.contains("ফুল")) Color(0xFF6424B2) else Color(0xFF15583E),
                    fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines=1, overflow=TextOverflow.Ellipsis)
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(.35f)), Alignment.Center) {
                    Text("→", fontSize=21.sp, color=Ink)
                }
            }
        }
    }
}


private data class CropItem(val id: String, val label: String)

private val CROP_ITEMS = listOf(
    CropItem("rice", "Rice"),
    CropItem("peanut", "Peanut"),
    CropItem("vegetables", "Vegetables"),
    CropItem("flowers", "Flowers")
)

private val RICE_VARIETIES = listOf(
    "Basmati", "Pusa Basmati 1121", "Pusa Basmati 1509", "Pusa Basmati 1718", "Sona Masuri",
    "IR64", "Swarna", "Gobindobhog", "Ponni", "HMT", "MTU 1010", "MTU 7029",
    "Samba Mahsuri", "BPT 5204", "PR 106", "PR 114", "ADT 36", "ADT 43", "ADT 45",
    "CR 1009", "CR 1009 Sub1", "Jaya", "Jyothi", "Uma", "Kalanamak", "Kala Namak"
)
private val PEANUT_VARIETIES = listOf(
    "JL 24", "GG 20", "TMV 2", "TAG 24", "Kadiri 6", "Kadiri 9", "Kadiri 2",
    "ICGS 11", "ICGS 44", "Dharani", "Kadiri Harithandhra", "TMV 7", "VRI 2"
)
private val VEGETABLE_CROPS = listOf(
    "Potato", "Tomato", "Brinjal", "Chilli", "Okra", "Cabbage", "Cauliflower", "Onion",
    "Cucumber", "Pumpkin", "Bitter Gourd", "Bottle Gourd", "Beans", "Peas", "Carrot",
    "Radish", "Spinach", "Amaranth", "Beetroot", "Turnip", "Broccoli", "Capsicum",
    "French Bean", "Cluster Bean", "Drumstick", "Ivy Gourd", "Ridge Gourd", "Snake Gourd",
    "Ash Gourd", "Pointed Gourd", "Coriander", "Fenugreek", "Sweet Potato", "Yam",
    "Colocasia", "Garlic", "Ginger", "Green Peas", "Sweet Corn", "Mushroom"
)
private val FLOWER_CROPS = listOf(
    "Rose", "Marigold", "Chrysanthemum", "Jasmine", "Tuberose", "Hibiscus", "Gerbera",
    "Gladiolus", "Lily", "Orchid", "Carnation", "Dahlia", "Aster", "Zinnia",
    "Petunia", "Gomphrena", "Celosia", "Crossandra", "Anthurium",
    "Sunflower", "Calendula", "Portulaca", "Periwinkle", "Rajnigandha"
)


private fun tx(language: String, en: String, bn: String, hi: String, ta: String, pa: String, te: String): String = when (language) {
    "বাংলা" -> bn; "हिन्दी" -> hi; "தமிழ்" -> ta; "ਪੰਜਾਬੀ" -> pa; "తెలుగు" -> te; else -> en
}

private fun ui(language: String, key: String): String = when (key) {
    "home" -> mapOf("বাংলা" to "হোম", "हिन्दी" to "होम", "தமிழ்" to "முகப்பு", "ਪੰਜਾਬੀ" to "ਹੋਮ", "తెలుగు" to "హోమ్", "English" to "Home")[language] ?: "Home"
    "history" -> mapOf("বাংলা" to "ইতিহাস", "हिन्दी" to "इतिहास", "தமிழ்" to "வரலாறு", "ਪੰਜਾਬੀ" to "ਇਤਿਹਾਸ", "తెలుగు" to "చరిత్ర", "English" to "History")[language] ?: "History"
    "profile" -> mapOf("বাংলা" to "প্রোফাইল", "हिन्दी" to "प्रोफ़ाइल", "தமிழ்" to "சுயவிவரம்", "ਪੰਜਾਬੀ" to "ਪ੍ਰੋਫਾਈਲ", "తెలుగు" to "ప్రొఫైల్", "English" to "Profile")[language] ?: "Profile"
    "greeting" -> mapOf("বাংলা" to "নমস্কার, কৃষক বন্ধু!", "हिन्दी" to "नमस्ते, किसान मित्र!", "தமிழ்" to "வணக்கம், விவசாயி நண்பரே!", "ਪੰਜਾਬੀ" to "ਸਤ ਸ੍ਰੀ ਅਕਾਲ, ਕਿਸਾਨ ਦੋਸਤ!", "తెలుగు" to "నమస్కారం, రైతు మిత్రమా!", "English" to "Hello, Farmer!")[language] ?: "Hello, Farmer!"
    "quick_help" -> mapOf("বাংলা" to "আপনার জন্য দ্রুত সাহায্য", "हिन्दी" to "आपके लिए त्वरित सहायता", "தமிழ்" to "உங்களுக்கான விரைவு உதவி", "ਪੰਜਾਬੀ" to "ਤੁਹਾਡੇ ਲਈ ਤੁਰੰਤ ਮਦਦ", "తెలుగు" to "మీ కోసం త్వరిత సహాయం", "English" to "Quick help for you")[language] ?: "Quick help for you"
    "diagnosis" -> mapOf("বাংলা" to "ফসল রোগ শনাক্তকরণ", "हिन्दी" to "फसल रोग पहचान", "தமிழ்" to "பயிர் நோய் கண்டறிதல்", "ਪੰਜਾਬੀ" to "ਫਸਲ ਰੋਗ ਪਛਾਣ", "తెలుగు" to "పంట వ్యాధి నిర్ధారణ", "English" to "Crop diagnosis")[language] ?: "Crop diagnosis"
    "voice" -> mapOf("বাংলা" to "ভয়েস পরামর্শ", "हिन्दी" to "वॉइस सलाह", "தமிழ்" to "குரல் ஆலோசனை", "ਪੰਜਾਬੀ" to "ਵੌਇਸ ਸਲਾਹ", "తెలుగు" to "వాయిస్ సలహా", "English" to "Voice advisory")[language] ?: "Voice advisory"
    "weather" -> mapOf("বাংলা" to "আবহাওয়া আপডেট", "हिन्दी" to "मौसम अपडेट", "தமிழ்" to "வானிலை புதுப்பிப்பு", "ਪੰਜਾਬੀ" to "ਮੌਸਮ ਅੱਪਡੇਟ", "తెలుగు" to "వాతావరణ నవీకరణ", "English" to "Weather update")[language] ?: "Weather update"
    "advisory" -> mapOf("বাংলা" to "ফসলের পরামর্শ", "हिन्दी" to "फसल सलाह", "தமிழ்" to "பயிர் ஆலோசனை", "ਪੰਜਾਬੀ" to "ਫਸਲ ਸਲਾਹ", "తెలుగు" to "పంట సలహా", "English" to "Crop advisory")[language] ?: "Crop advisory"
    "popular" -> mapOf("বাংলা" to "জনপ্রিয় ফসল", "हिन्दी" to "लोकप्रिय फसलें", "தமிழ்" to "பிரபலமான பயிர்கள்", "ਪੰਜਾਬੀ" to "ਪ੍ਰਸਿੱਧ ਫਸਲਾਂ", "తెలుగు" to "ప్రసిద్ధ పంటలు", "English" to "Popular crops")[language] ?: "Popular crops"
    "profile_language" -> mapOf("বাংলা" to "ভাষা নির্বাচন করুন", "हिन्दी" to "भाषा चुनें", "தமிழ்" to "மொழியைத் தேர்ந்தெடுக்கவும்", "ਪੰਜਾਬੀ" to "ਭਾਸ਼ਾ ਚੁਣੋ", "తెలుగు" to "భాషను ఎంచుకోండి", "English" to "Choose language")[language] ?: "Choose language"
    "farmer_profile" -> mapOf("বাংলা" to "🌾 কৃষক প্রোফাইল", "हिन्दी" to "🌾 किसान प्रोफ़ाइल", "தமிழ்" to "🌾 விவசாயி சுயவிவரம்", "ਪੰਜਾਬੀ" to "🌾 ਕਿਸਾਨ ਪ੍ਰੋਫਾਈਲ", "తెలుగు" to "🌾 రైతు ప్రొఫైల్", "English" to "🌾 Farmer profile")[language] ?: "🌾 Farmer profile"
    "no_history" -> mapOf("বাংলা" to "এখনও কোনও বিশ্লেষণ সংরক্ষিত নেই।", "हिन्दी" to "अभी कोई विश्लेषण सहेजा नहीं गया है।", "தமிழ்" to "இன்னும் பகுப்பாய்வுகள் சேமிக்கப்படவில்லை.", "ਪੰਜਾਬੀ" to "ਹਾਲੇ ਕੋਈ ਵਿਸ਼ਲੇਸ਼ਣ ਸੁਰੱਖਿਅਤ ਨਹੀਂ ਹੈ।", "తెలుగు" to "ఇంకా విశ్లేషణలు సేవ్ కాలేదు.", "English" to "No saved analyses yet.")[language] ?: "No saved analyses yet."
    "history_desc" -> mapOf("বাংলা" to "আপনার আগের AI পরামর্শ এখানে দেখা যাবে।", "हिन्दी" to "आपकी पिछली AI सलाह यहाँ दिखाई देगी।", "தமிழ்" to "உங்கள் முந்தைய AI ஆலோசனைகள் இங்கே தோன்றும்.", "ਪੰਜਾਬੀ" to "ਤੁਹਾਡੀ ਪਿਛਲੀ AI ਸਲਾਹ ਇੱਥੇ ਦਿਖਾਈ ਦੇਵੇਗੀ।", "తెలుగు" to "మీ గత AI సలహాలు ఇక్కడ కనిపిస్తాయి.", "English" to "Your previous AI advice will appear here.")[language] ?: "Your previous AI advice will appear here."
    "popular_crops" -> mapOf("বাংলা" to "জনপ্রিয় ফসল", "हिन्दी" to "लोकप्रिय फसलें", "தமிழ்" to "பிரபலமான பயிர்கள்", "ਪੰਜਾਬੀ" to "ਪ੍ਰਸித்தமான பயிர்கள்", "తెలుగు" to "ప్రసిద్ధ పంటలు", "English" to "Popular crops")[language] ?: "Popular crops"
    "select_language" -> mapOf("বাংলা" to "ভাষা নির্বাচন করুন", "हिन्दी" to "भाषा चुनें", "தமிழ்" to "மொழியைத் தேர்ந்தெடுக்கவும்", "ਪੰਜਾਬੀ" to "ਭਾਸ਼ਾ ਚੁਣੋ", "తెలుగు" to "భాషను ఎంచుకోండి", "English" to "Choose language")[language] ?: "Choose language"
    else -> key
}

private fun cropLabel(crop: String, language: String): String = when (crop) {
    "rice" -> when(language) { "বাংলা" -> "ধান"; "हिन्दी" -> "धान"; "தமிழ்" -> "நெல்"; "ਪੰਜਾਬੀ" -> "ਚੌਲ"; "తెలుగు" -> "వరి"; else -> "Rice" }
    "peanut" -> when(language) { "বাংলা" -> "বাদাম"; "हिन्दी" -> "मूंगफली"; "தமிழ்" -> "நிலக்கடலை"; "ਪੰਜਾਬੀ" -> "ਮੂੰਗਫਲੀ"; "తెలుగు" -> "వేరుశెనగ"; else -> "Peanut" }
    "vegetables" -> when(language) { "বাংলা" -> "সবজি"; "हिन्दी" -> "सब्ज़ियाँ"; "தமிழ்" -> "காய்கறிகள்"; "ਪੰਜਾਬੀ" -> "ਸਬਜ਼ੀਆਂ"; "తెలుగు" -> "కూరగాయలు"; else -> "Vegetables" }
    "flowers" -> when(language) { "বাংলা" -> "ফুল"; "हिन्दी" -> "फूल"; "தமிழ்" -> "மலர்கள்"; "ਪੰਜਾਬੀ" -> "ਫੁੱਲ"; "తెలుగు" -> "పూలు"; else -> "Flowers" }
    else -> crop
}

private fun cropSearchItems(category: String): List<String> = when(category) {
    "rice" -> RICE_VARIETIES
    "peanut" -> PEANUT_VARIETIES
    "vegetables" -> VEGETABLE_CROPS
    "flowers" -> FLOWER_CROPS
    else -> emptyList()
}


@Composable
private fun CropSelector(language: String, selectedCrop: String, onCropChange: (String) -> Unit, selectedCropName: String, onCropNameChange: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    val items = cropSearchItems(selectedCrop)
    val filtered = items.filter { it.contains(search.trim(), ignoreCase = true) }

    val title = when (language) {
        "বাংলা" -> "ফসল ও জাত নির্বাচন করুন"
        "हिन्दी" -> "फसल और किस्म चुनें"
        "தமிழ்" -> "பயிர் மற்றும் ரகத்தைத் தேர்ந்தெடுக்கவும்"
        "ਪੰਜਾਬੀ" -> "ਫਸਲ ਅਤੇ ਕਿਸਮ ਚੁਣੋ"
        "తెలుగు" -> "పంట మరియు రకాన్ని ఎంచుకోండి"
        else -> "Select crop and variety"
    }
    val placeholder = when (language) {
        "বাংলা" -> "জাত / ফসল খুঁজুন"
        "हिन्दी" -> "किस्म / फसल खोजें"
        "தமிழ்" -> "ரகம் / பயிரைத் தேடுங்கள்"
        "ਪੰਜਾਬੀ" -> "ਕਿਸਮ / ਫਸਲ ਖੋਜੋ"
        "తెలుగు" -> "రకం / పంటను వెతకండి"
        else -> "Search variety / crop"
    }
    val noResults = when (language) {
        "বাংলা" -> "কোনও ফল পাওয়া যায়নি"
        "हिन्दी" -> "कोई परिणाम नहीं मिला"
        "தமிழ்" -> "முடிவுகள் எதுவும் இல்லை"
        "ਪੰਜਾਬੀ" -> "ਕੋਈ ਨਤੀਜਾ ਨਹੀਂ ਮਿਲਿਆ"
        "తెలుగు" -> "ఫలితాలు ఏవీ లేవు"
        else -> "No matching crop or variety"
    }

    Column {
        Text(title, color=Ink, fontSize=16.sp, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(CROP_ITEMS) { item ->
                FilterChip(
                    selected = selectedCrop == item.id,
                    onClick = { onCropChange(item.id); search = "" },
                    label = { Text(cropLabel(item.id, language), fontSize=12.sp) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value=search,
            onValueChange={ search = it },
            modifier=Modifier.fillMaxWidth(),
            singleLine=true,
            placeholder={ Text(placeholder) }
        )
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Text(noResults, color=Muted, fontSize=12.sp, modifier=Modifier.padding(vertical=8.dp))
        } else {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                items(filtered) { item ->
                    AssistChip(
                        onClick={ search = item; onCropNameChange(item) },
                        label={ Text(if(selectedCropName==item) "✓ $item" else item, fontSize=11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(modifier: Modifier, icon: String, title: String, subtitle: String, tint: Color, iconTint: Color, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) { Column(Modifier.padding(14.dp)) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(tint), Alignment.Center) { Text(icon, color = iconTint, fontSize = 22.sp) }; Spacer(Modifier.height(12.dp)); Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(Modifier.height(4.dp)); Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp) } }
}

@Composable
private fun DiagnosisScreen(language: String, selectedCrop: String, onCropChange: (String) -> Unit, selectedCropName: String, onCropNameChange: (String) -> Unit, selectedImage: Bitmap?, imageSource: ImageSource?, cameraDenied: Boolean, onBack: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit, onClear: () -> Unit, onAnalyze: () -> Unit, onVoice: () -> Unit) {
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
private fun ResultScreen(selectedImage: Bitmap?, selectedCrop: String, selectedCropName: String, language: String, result: AdvisoryResult?, error: String?, analyzing: Boolean, onBack: () -> Unit, onRetry: () -> Unit, onVoice: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("‹", fontSize = 36.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack)); Text("বিশ্লেষণের ফলাফল", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(14.dp))
        if (selectedImage != null) { Image(selectedImage.asImageBitmap(), "Crop", Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop); Spacer(Modifier.height(14.dp)) }
        when {
            analyzing -> { Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = KrishiGreen); Spacer(Modifier.height(14.dp)); Text("OpenKrishi AI থেকে পরামর্শ আনা হচ্ছে…", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text("আপনার নির্বাচিত ভাষায় উত্তর প্রস্তুত হচ্ছে।", color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp) } } }
            error != null -> { ResultCard("⚠️", "সংযোগ সমস্যা", "পরামর্শ আনা যায়নি: $error"); Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(KrishiGreen)) { Text("আবার চেষ্টা করুন") } }
            result != null -> { ResultCard("🌾", if(language=="English") "Crop" else "ফসল", cropLabel(selectedCrop, language) + if(selectedCropName.isNotBlank()) " • $selectedCropName" else ""); if (result.observations.isNotEmpty()) ResultCard("👁️", "ছবিতে দেখা লক্ষণ", result.observations.joinToString("\n• ", prefix = "• ")); if (result.possibleCauses.isNotEmpty()) ResultCard("🔎", "সম্ভাব্য কারণ", result.possibleCauses.joinToString("\n• ", prefix = "• ")); ResultCard("💡", "AI পরামর্শ", result.answer); if (result.recommendations.isNotEmpty()) ResultCard("✓", "নিরাপদ পরবর্তী পদক্ষেপ", result.recommendations.joinToString("\n• ", prefix = "• ")); ResultCard("◉", "Confidence", result.confidence); ResultCard("✓", "Safety", result.safety) }
            else -> ResultCard("ℹ️", "অপেক্ষা করুন", "বিশ্লেষণ শুরু করা হয়নি।")
        }
        Spacer(Modifier.height(10.dp)); Button(onClick = onVoice, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(KrishiGreen)) { Text("🔊 পরামর্শ শুনুন", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp)); Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(KrishiMint), modifier = Modifier.fillMaxWidth()) { Text("ছবিটি AI Vision-এ পাঠিয়ে নির্বাচিত ভাষায় ফলাফল তৈরি করা হয়েছে।", color = KrishiDeep, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(13.dp)) }
    }
}

@Composable
private fun ResultCard(icon: String, title: String, body: String) { Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) { Text(icon, fontSize = 22.sp); Spacer(Modifier.width(10.dp)); Column { Text(title, color = KrishiGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(body, color = Ink, fontSize = 14.sp, lineHeight = 20.sp) } } } }

@Composable
private fun VoiceScreen(language: String, selectedCrop: String, onCropChange: (String) -> Unit, selectedCropName: String, onCropNameChange: (String) -> Unit, onBack: () -> Unit) {
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
                    val r = OpenKrishiApi.getAdvisory(text, language, selectedCrop, selectedCropName)
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
        TopBar(ui(language, "voice"), language, onBack)
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
private fun AdvisoryInputScreen(language:String, selectedCrop:String, onCropChange:(String)->Unit, selectedCropName:String, onCropNameChange:(String)->Unit, query:String, onQuery:(String)->Unit, onAsk:(String)->Unit, onBack:()->Unit) {
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
private fun HistoryScreen(modifier: Modifier, language: String) { Column(modifier.fillMaxSize().padding(18.dp)) { Text(ui(language, "history"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(8.dp)); Text(ui(language, "history_desc"), color = Muted, fontSize = 13.sp); Spacer(Modifier.height(18.dp)); Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) { Text(ui(language, "no_history"), color = Muted, modifier = Modifier.padding(18.dp)) } } }

@Composable
private fun ProfileScreen(modifier: Modifier, language: String, onLanguage: (String) -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text(ui(language, "profile"), fontSize=24.sp, fontWeight=FontWeight.Bold, color=Ink)
        Spacer(Modifier.height(18.dp))
        Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(Color.White), modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(ui(language, "farmer_profile"), fontSize=18.sp, fontWeight=FontWeight.Bold, color=Ink)
                Spacer(Modifier.height(8.dp)); Text("${ui(language, "select_language")}: $language", color=Muted, fontSize=13.sp)
                Spacer(Modifier.height(12.dp)); Text(ui(language, "select_language"), color=Ink, fontWeight=FontWeight.Bold)
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