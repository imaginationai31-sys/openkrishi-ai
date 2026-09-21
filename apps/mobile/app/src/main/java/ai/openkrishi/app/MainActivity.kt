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
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

private val Green = Color(0xFF087443)
private val DeepGreen = Color(0xFF06452F)
private val Leaf = Color(0xFF55A85F)
private val Mint = Color(0xFFE8F6EC)
private val Sky = Color(0xFFEAF5FF)
private val Purple = Color(0xFFF1EAFF)
private val Amber = Color(0xFFFFF5D9)
private val Ink = Color(0xFF13231B)
private val Muted = Color(0xFF68756D)
private val Background = Color(0xFFF6FAF7)

private enum class AppTab { HOME, HISTORY, PROFILE }
private enum class Screen { HOME, ONBOARDING, DIAGNOSIS, RESULT, ADVISORY, WEATHER, VOICE }
private enum class ImageSource { CAMERA, GALLERY }

private data class HistoryEntry(val title: String, val subtitle: String, val icon: String)
private data class CropItem(val id: String, val label: String)

private val LANG_OPTIONS = listOf("English", "বাংলা", "हिन्दी", "தமிழ்", "ਪੰਜਾਬੀ", "తెలుగు")
private val CROP_ITEMS = listOf(
    CropItem("rice", "Rice"),
    CropItem("peanut", "Peanut"),
    CropItem("vegetables", "Vegetables"),
    CropItem("flowers", "Flowers")
)
private val GROWTH_STAGES = listOf("Seedling", "Vegetative", "Tillering", "Flowering", "Fruiting", "Harvest")
private val RICE_VARIETIES = listOf("Basmati", "Pusa Basmati 1121", "Pusa Basmati 1509", "Sona Masuri", "IR64", "Swarna", "Gobindobhog", "MTU 1010", "Samba Mahsuri")
private val PEANUT_VARIETIES = listOf("JL 24", "GG 20", "TMV 2", "TAG 24", "Kadiri 6", "Kadiri 9", "Dharani")
private val VEGETABLE_CROPS = listOf("Potato", "Tomato", "Brinjal", "Chilli", "Okra", "Cabbage", "Cauliflower", "Onion", "Cucumber", "Pumpkin", "Bitter Gourd", "Bottle Gourd", "Beans", "Carrot", "Radish", "Spinach", "Capsicum")
private val FLOWER_CROPS = listOf("Rose", "Marigold", "Chrysanthemum", "Jasmine", "Tuberose", "Hibiscus", "Gerbera", "Gladiolus", "Lily", "Orchid", "Carnation", "Dahlia", "Sunflower", "Calendula")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenKrishiApp() }
    }
}

@Composable
private fun OpenKrishiApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            onPrimary = Color.White,
            background = Background,
            surface = Color.White
        )
    ) {
        val context = LocalContext.current
        var screen by remember { mutableStateOf(Screen.HOME) }
        var tab by remember { mutableStateOf(AppTab.HOME) }
        var language by remember { mutableStateOf("English") }
        var splashVisible by remember { mutableStateOf(true) }

        var selectedCrop by remember { mutableStateOf("rice") }
        var selectedCropName by remember { mutableStateOf("Basmati") }
        var growthStage by remember { mutableStateOf("Tillering") }
        var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
        var imageSource by remember { mutableStateOf<ImageSource?>(null) }
        var result by remember { mutableStateOf<AdvisoryResult?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var loading by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }
        var weather by remember { mutableStateOf<WeatherResult?>(null) }
        var weatherLoading by remember { mutableStateOf(false) }
        val history = remember { mutableStateListOf<HistoryEntry>() }

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1100)
            splashVisible = false
            val seen = context.getSharedPreferences("openkrishi", Context.MODE_PRIVATE).getBoolean("onboarded", false)
            if (!seen) screen = Screen.ONBOARDING
        }

        fun finishOnboarding() {
            context.getSharedPreferences("openkrishi", Context.MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
            screen = Screen.HOME
        }

        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImage = context.contentResolverBitmap(uri)
                imageSource = ImageSource.GALLERY
                result = null
                error = null
                screen = Screen.DIAGNOSIS
            }
        }
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                selectedImage = bitmap
                imageSource = ImageSource.CAMERA
                result = null
                error = null
                screen = Screen.DIAGNOSIS
            }
        }
        val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) cameraLauncher.launch(null)
            else error = tx(language, "Camera permission is required to scan a crop.", "ফসল স্ক্যান করতে ক্যামেরার অনুমতি দরকার।", "फसल स्कैन करने के लिए कैमरा अनुमति आवश्यक है।", "பயிரை ஸ்கேன் செய்ய கேமரா அனுமதி தேவை.", "ਫਸਲ ਸਕੈਨ ਕਰਨ ਲਈ ਕੈਮਰਾ ਇਜਾਜ਼ਤ ਲੋੜੀਂਦੀ ਹੈ।", "పంటను స్కాన్ చేయడానికి కెమెరా అనుమతి అవసరం.")
        }
        fun openCamera() {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraLauncher.launch(null)
            else cameraPermission.launch(Manifest.permission.CAMERA)
        }

        fun analyzeImage() {
            val image = selectedImage ?: return
            loading = true
            error = null
            result = null
            screen = Screen.RESULT
            Thread {
                try {
                    val r = OpenKrishiApi.assessImage(image, language, selectedCrop, growthStage, selectedCropName)
                    Handler(Looper.getMainLooper()).post {
                        result = r
                        loading = false
                        history.add(0, HistoryEntry("Crop diagnosis", selectedCropName + " • " + growthStage, "📷"))
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { error = e.message ?: "AI service is unavailable."; loading = false }
                }
            }.start()
        }

        fun askAdvisory(text: String) {
            if (text.isBlank()) return
            query = text
            loading = true
            error = null
            result = null
            screen = Screen.ADVISORY
            Thread {
                try {
                    val r = OpenKrishiApi.getAdvisory(text, language, selectedCrop, selectedCropName)
                    Handler(Looper.getMainLooper()).post {
                        result = r
                        loading = false
                        history.add(0, HistoryEntry("AI advisory", text.take(48), "💡"))
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { error = e.message ?: "AI service is unavailable."; loading = false }
                }
            }.start()
        }

        fun loadWeather() {
            weatherLoading = true
            error = null
            screen = Screen.WEATHER
            Thread {
                try {
                    val w = OpenKrishiApi.getWeather(22.5726, 88.3639, language)
                    Handler(Looper.getMainLooper()).post { weather = w; weatherLoading = false }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { error = e.message ?: "Weather service is unavailable."; weatherLoading = false }
                }
            }.start()
        }

        if (splashVisible) {
            SplashScreen()
            return
        }

        when (screen) {
            Screen.ONBOARDING -> OnboardingScreen(language, { language = it }, ::finishOnboarding)
            Screen.DIAGNOSIS -> DiagnosisScreen(
                language, selectedCrop, { selectedCrop = it; selectedCropName = cropOptions(it).firstOrNull().orEmpty() },
                selectedCropName, { selectedCropName = it }, growthStage, { growthStage = it },
                selectedImage, imageSource, error,
                { screen = Screen.HOME }, { galleryLauncher.launch("image/*") }, ::openCamera,
                { selectedImage = null; imageSource = null; result = null; error = null },
                ::analyzeImage, { screen = Screen.VOICE }
            )
            Screen.RESULT -> ResultScreen(
                language, selectedCropName, growthStage, selectedImage, result, error, loading,
                { screen = Screen.DIAGNOSIS }, ::analyzeImage, { screen = Screen.VOICE }
            )
            Screen.ADVISORY -> {
                if (result == null && !loading) AdvisoryInputScreen(
                    language, selectedCrop, { selectedCrop = it; selectedCropName = cropOptions(it).firstOrNull().orEmpty() },
                    selectedCropName, { selectedCropName = it }, query, { query = it }, ::askAdvisory
                ) { screen = Screen.HOME }
                else ResultScreen(language, selectedCropName, growthStage, null, result, error, loading, { screen = Screen.HOME }, { askAdvisory(query) }, { screen = Screen.VOICE })
            }
            Screen.WEATHER -> WeatherScreen(language, weather, weatherLoading, error, ::loadWeather) { screen = Screen.HOME }
            Screen.VOICE -> VoiceScreen(language, selectedCrop, { selectedCrop = it; selectedCropName = cropOptions(it).firstOrNull().orEmpty() }, selectedCropName, { selectedCropName = it }, ::askAdvisory) { screen = Screen.HOME }
            Screen.HOME -> Scaffold(
                containerColor = Background,
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(tab == AppTab.HOME, { tab = AppTab.HOME }, icon = { Text("⌂", fontSize = 24.sp) }, label = { Text(ui(language, "home")) })
                        NavigationBarItem(tab == AppTab.HISTORY, { tab = AppTab.HISTORY }, icon = { Text("◷", fontSize = 22.sp) }, label = { Text(ui(language, "history")) })
                        NavigationBarItem(tab == AppTab.PROFILE, { tab = AppTab.PROFILE }, icon = { Text("○", fontSize = 24.sp) }, label = { Text(ui(language, "profile")) })
                    }
                }
            ) { padding ->
                when (tab) {
                    AppTab.HOME -> HomeScreen(Modifier.padding(padding), language, { language = it }, { crop ->
                        selectedCrop = crop
                        selectedCropName = cropOptions(crop).firstOrNull().orEmpty()
                        screen = Screen.DIAGNOSIS
                    }, { screen = Screen.VOICE }, ::loadWeather, { screen = Screen.ADVISORY })
                    AppTab.HISTORY -> HistoryScreen(Modifier.padding(padding), language, history)
                    AppTab.PROFILE -> ProfileScreen(Modifier.padding(padding), language, { language = it })
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DeepGreen, Green, Leaf))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(104.dp).clip(RoundedCornerShape(32.dp)).background(Color.White.copy(.14f)), contentAlignment = Alignment.Center) {
                Text("🌿", fontSize = 58.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text("OpenKrishi AI", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("AI advice in your language", color = Color.White.copy(.82f), fontSize = 15.sp)
            Spacer(Modifier.height(34.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun OnboardingScreen(language: String, onLanguage: (String) -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Background)) {
        Box(Modifier.fillMaxWidth().height(310.dp).background(Brush.verticalGradient(listOf(DeepGreen, Green, Leaf)))) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌾", fontSize = 70.sp)
                Spacer(Modifier.height(12.dp))
                Text("OpenKrishi AI", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("AI advice in your language", color = Color.White.copy(.85f), fontSize = 15.sp)
            }
        }
        Column(Modifier.padding(22.dp)) {
            Text(tx(language, "Smarter farming starts here", "স্মার্ট কৃষির শুরু এখান থেকে", "स्मार्ट खेती की शुरुआत यहाँ से", "ஸ்மார்ட் விவசாயம் இங்கிருந்து தொடங்குகிறது", "ਸਮਾਰਟ ਖੇਤੀ ਦੀ ਸ਼ੁਰੂਆਤ ਇੱਥੋਂ", "స్మార్ట్ వ్యవసాయం ఇక్కడ ప్రారంభమవుతుంది"), color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(tx(language, "Take a crop photo, ask by voice, understand the weather and get practical AI guidance.", "ফসলের ছবি তুলুন, কণ্ঠে প্রশ্ন করুন, আবহাওয়া বুঝুন এবং AI পরামর্শ নিন।", "फसल की फोटो लें, आवाज़ में पूछें, मौसम समझें और AI सलाह लें।", "பயிர் புகைப்படம் எடுத்து, குரலில் கேட்டு, வானிலையை அறிந்து AI ஆலோசனை பெறுங்கள்.", "ਫਸਲ ਦੀ ਤਸਵੀਰ ਲਓ, ਆਵਾਜ਼ ਵਿੱਚ ਪੁੱਛੋ, ਮੌਸਮ ਜਾਣੋ ਅਤੇ AI ਸਲਾਹ ਲਓ।", "పంట ఫోటో తీసి, వాయిస్‌లో అడిగి, వాతావరణాన్ని తెలుసుకుని AI సలహా పొందండి."), color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(20.dp))
            Text("Choose your language", color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LANG_OPTIONS) { option -> FilterChip(selected = language == option, onClick = { onLanguage(option) }, label = { Text(option, fontSize = 11.sp) }) }
            }
            Spacer(Modifier.height(22.dp))
            FeatureLine("📷", tx(language, "Photo diagnosis", "ছবি দিয়ে বিশ্লেষণ", "फोटो से विश्लेषण", "புகைப்பட ஆய்வு", "ਤਸਵੀਰ ਨਾਲ ਵਿਸ਼ਲੇਸ਼ਣ", "ఫోటో విశ్లేషణ"))
            FeatureLine("🎙️", tx(language, "Voice advice", "ভয়েস পরামর্শ", "वॉइस सलाह", "குரல் ஆலோசனை", "ਵੌਇਸ ਸਲਾਹ", "వాయిస్ సలహా"))
            FeatureLine("☀️", tx(language, "Weather alerts", "আবহাওয়া সতর্কতা", "मौसम चेतावनी", "வானிலை எச்சரிக்கை", "ਮੌਸਮ ਚੇਤਾਵਨੀਆਂ", "వాతావరణ హెచ్చరికలు"))
            Spacer(Modifier.height(22.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
                Text(tx(language, "Get started", "শুরু করুন", "शुरू करें", "தொடங்குங்கள்", "ਸ਼ੁਰੂ ਕਰੋ", "ప్రారంభించండి"), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text("OpenKrishi AI • Smart Farming for a Better Tomorrow", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FeatureLine(icon: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) { Text(icon, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Text(text, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, language: String, onLanguage: (String) -> Unit, onDiagnosis: (String) -> Unit, onVoice: () -> Unit, onWeather: () -> Unit, onAdvice: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(DeepGreen, Green, Color(0xFF78BE72)))) ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.14f)), contentAlignment = Alignment.Center) { Text("🌿", fontSize = 24.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("OpenKrishi AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(tx(language, "Smart farming • Better tomorrow", "স্মার্ট কৃষি • উন্নত আগামীকাল", "स्मार्ट खेती • बेहतर कल", "ஸ்மார்ட் விவசாயம் • சிறந்த நாளை", "ਸਮਾਰਟ ਖੇਤੀ • ਬਿਹਤਰ ਕੱਲ੍ਹ", "స్మార్ట్ వ్యవసాయం • మంచి రేపు"), color = Color.White.copy(.78f), fontSize = 11.sp)
                    }
                    Text(language, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(18.dp))
                Text(tx(language, "Hello, Farmer! 👋", "নমস্কার, কৃষক বন্ধু! 👋", "नमस्ते, किसान मित्र! 👋", "வணக்கம், விவசாயி நண்பரே! 👋", "ਸਤ ਸ੍ਰੀ ਅਕਾਲ, ਕਿਸਾਨ ਮਿੱਤਰ! 👋", "నమస్కారం, రైతు మిత్రమా! 👋"), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LANG_OPTIONS) { option -> FilterChip(selected = language == option, onClick = { onLanguage(option) }, label = { Text(option, fontSize = 9.sp) }) }
                }
                Spacer(Modifier.height(10.dp))
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White.copy(.98f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🌤️", fontSize = 35.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("26°C", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(tx(language, "Partly cloudy", "আংশিক মেঘলা", "आंशिक बादल", "ஓரளவு மேகமூட்டம்", "ਅੰਸ਼ਿਕ ਬੱਦਲ", "పాక్షికంగా మేఘావృతం"), color = Muted, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(tx(language, "Kolkata, West Bengal", "কলকাতা, পশ্চিমবঙ্গ", "कोलकाता, पश्चिम बंगाल", "கொல்கத்தா, மேற்கு வங்காளம்", "ਕੋਲਕਾਤਾ, ਪੱਛਮੀ ਬੰਗਾਲ", "కోల్‌కతా, పశ్చిమ బెంగాల్"), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("82% • 12 km/h", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(18.dp)) {
            Text(tx(language, "Everything you need", "আপনার দরকারি সবকিছু", "आपके लिए सभी ज़रूरी सुविधाएँ", "உங்களுக்கு தேவையான அனைத்தும்", "ਤੁਹਾਡੀ ਲੋੜ ਦੀ ਹਰ ਚੀਜ਼", "మీకు కావాల్సిన ప్రతిదీ"), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureCard(Modifier.weight(1f), "📷", ui(language, "diagnosis"), tx(language, "Scan a crop photo", "ফসলের ছবি স্ক্যান করুন", "फसल की फोटो स्क্যান करें", "பயிர் புகைப்படத்தை ஸ்கேன் செய்யுங்கள்", "ਫਸਲ ਦੀ ਤਸਵੀਰ ਸਕੈਨ ਕਰੋ", "పంట ఫోటో స్కాన్ చేయండి"), Mint, { onDiagnosis("rice") })
                FeatureCard(Modifier.weight(1f), "🎙️", ui(language, "voice"), tx(language, "Speak and hear AI advice", "কথা বলুন, AI পরামর্শ শুনুন", "बोलें और AI सलाह सुनें", "பேசி AI ஆலோசனை கேளுங்கள்", "ਬੋਲੋ ਅਤੇ AI ਸਲਾਹ ਸੁਣੋ", "మాట్లాడి AI సలహా వినండి"), Purple, onVoice)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureCard(Modifier.weight(1f), "☀️", ui(language, "weather"), tx(language, "7-day forecast & alerts", "৭ দিনের পূর্বাভাস", "7 दिन का पूर्वानुमान", "7 நாள் வானிலை", "7 ਦਿਨਾਂ ਦੀ ਭਵਿੱਖਬਾਣੀ", "7 రోజుల వాతావరణం"), Sky, onWeather)
                FeatureCard(Modifier.weight(1f), "💡", ui(language, "advisory"), tx(language, "Ask about your crop", "ফসল সম্পর্কে প্রশ্ন করুন", "अपनी फसल के बारे में पूछें", "உங்கள் பயிரைப் பற்றி கேளுங்கள்", "ਆਪਣੀ ਫਸਲ ਬਾਰੇ ਪੁੱਛੋ", "మీ పంట గురించి అడగండి"), Amber, onAdvice)
            }
            Spacer(Modifier.height(24.dp))
            Text(tx(language, "Popular crops", "জনপ্রিয় ফসল", "लोकप्रिय फसलें", "பிரபலமான பயிர்கள்", "ਪ੍ਰਸਿੱਧ ਫਸਲਾਂ", "ప్రసిద్ధ పంటలు"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CROP_ITEMS) { crop ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.width(92.dp).clickable { onDiagnosis(crop.id) }) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(cropIcon(crop.id), fontSize = 30.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(crop.label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Mint), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(tx(language, "In the farmer's language, by the farmer's side", "কৃষকের ভাষায়, কৃষকের পাশে", "किसान की भाषा में, किसान के साथ", "விவசாயியின் மொழியில், விவசாயியின் பக்கத்தில்", "ਕਿਸਾਨ ਦੀ ਭਾਸ਼ਾ ਵਿੱਚ, ਕਿਸਾਨ ਦੇ ਨਾਲ", "రైతు భాషలో, రైతు పక్కన"), color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(tx(language, "AI Vision • Voice • Weather • 6 languages", "AI Vision • Voice • Weather • ৬টি ভাষা", "AI Vision • Voice • Weather • 6 भाषाएँ", "AI Vision • Voice • Weather • 6 மொழிகள்", "AI Vision • Voice • Weather • 6 ਭਾਸ਼ਾਵਾਂ", "AI Vision • Voice • Weather • 6 భాషలు"), color = Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(modifier: Modifier, icon: String, title: String, subtitle: String, bg: Color, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(bg)) {
        Column(Modifier.padding(15.dp).heightIn(min = 128.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(.72f)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 22.sp) }
            Spacer(Modifier.height(10.dp))
            Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Muted, fontSize = 10.sp, lineHeight = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DiagnosisScreen(
    language: String, crop: String, onCropChange: (String) -> Unit,
    cropName: String, onCropNameChange: (String) -> Unit,
    stage: String, onStageChange: (String) -> Unit,
    image: Bitmap?, source: ImageSource?, error: String?,
    onBack: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit, onRemove: () -> Unit,
    onAnalyze: () -> Unit, onVoice: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(ui(language, "diagnosis"), language, onBack)
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CROP_ITEMS) { item -> AssistChip(onClick = { onCropChange(item.id) }, label = { Text(item.label) }, leadingIcon = { Text(cropIcon(item.id)) }) }
        }
        Spacer(Modifier.height(10.dp))
        DropdownLike("Crop variety / crop", cropName, cropOptions(crop), onCropNameChange)
        Spacer(Modifier.height(10.dp))
        DropdownLike("Growth stage", stage, GROWTH_STAGES, onStageChange)
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
            if (image != null) {
                Box(Modifier.fillMaxWidth().height(360.dp)) {
                    Image(image.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Surface(Modifier.align(Alignment.TopEnd).padding(12.dp), shape = RoundedCornerShape(50), color = Color.Black.copy(.55f)) {
                        Text(if (source == ImageSource.CAMERA) "📷 Camera" else "🖼 Gallery", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().height(360.dp).padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("🌱", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(tx(language, "Take a clear photo of the affected part of the crop", "আক্রান্ত অংশের পরিষ্কার ছবি তুলুন", "प्रभावित हिस्से की साफ फोटो लें", "பாதிக்கப்பட்ட பகுதியின் தெளிவான புகைப்படம் எடுக்கவும்", "ਪ੍ਰਭਾਵਿਤ ਹਿੱਸੇ ਦੀ ਸਾਫ਼ ਤਸਵੀਰ ਲਓ", "ప్రభావిత భాగం యొక్క స్పష్టమైన ఫోటో తీయండి"), color = Ink, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onCamera, shape = RoundedCornerShape(14.dp)) { Text("📷 Camera") }
                        OutlinedButton(onClick = onGallery, shape = RoundedCornerShape(14.dp)) { Text("🖼 Gallery") }
                    }
                }
            }
        }
        if (image != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCamera, modifier = Modifier.weight(1f)) { Text("📷 Camera") }
                OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f)) { Text("🖼 Gallery") }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) {
                Text(tx(language, "Analyze crop", "ফসল বিশ্লেষণ করুন", "फसल का विश्लेषण करें", "பயிரை ஆய்வு செய்யுங்கள்", "ਫਸਲ ਦਾ ਵਿਸ਼ਲੇਸ਼ਣ ਕਰੋ", "పంటను విశ్లేషించండి"), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) { Text(tx(language, "Remove photo", "ছবি সরান", "फोटो हटाएँ", "புகைப்படத்தை அகற்றவும்", "ਤਸਵੀਰ ਹਟਾਓ", "ఫోటో తొలగించండి")) }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = onVoice, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("🎙️ " + tx(language, "Describe by voice instead", "ভয়েসে বলুন", "आवाज़ से बताएं", "குரலில் சொல்லுங்கள்", "ਵੌਇਸ ਨਾਲ ਦੱਸੋ", "వాయిస్‌తో చెప్పండి")) }
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFF9A5B00), fontSize = 12.sp) }
    }
}

@Composable
private fun DropdownLike(title: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember(value) { mutableStateOf(false) }
    Column {
        Text(title, color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedCard(onClick = { expanded = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(value.ifBlank { "Select" }, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("⌄", color = Green, fontSize = 18.sp)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
            }
        }
    }
}

@Composable
private fun ResultScreen(language: String, cropName: String, stage: String, image: Bitmap?, result: AdvisoryResult?, error: String?, loading: Boolean, onBack: () -> Unit, onRetry: () -> Unit, onVoice: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(ui(language, "diagnosis"), language, onBack)
        Spacer(Modifier.height(14.dp))
        if (image != null) {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Image(image.asImageBitmap(), null, Modifier.fillMaxWidth().height(170.dp), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.height(12.dp))
        }
        if (loading) {
            LoadingCard(tx(language, "Analyzing your crop…", "আপনার ফসল বিশ্লেষণ হচ্ছে…", "आपकी फसल का विश्लेषण हो रहा है…", "உங்கள் பயிர் ஆய்வு செய்யப்படுகிறது…", "ਤੁਹਾਡੀ ਫਸਲ ਦਾ ਵਿਸ਼ਲੇਸ਼ਣ ਹੋ ਰਿਹਾ ਹੈ…", "మీ పంటను విశ్లేషిస్తున్నాం…"))
        } else if (result != null) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Mint), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(cropName.ifBlank { "Crop" }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Growth stage: " + stage, color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Confidence • " + result.confidence, color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (result.answer.isNotBlank()) ResultCard("💡", ui(language, "advisory"), result.answer)
            if (result.observations.isNotEmpty()) ResultListCard("👁", ui(language, "observations"), result.observations)
            if (result.possibleCauses.isNotEmpty()) ResultListCard("⚠️", ui(language, "possible_causes"), result.possibleCauses)
            if (result.recommendations.isNotEmpty()) ResultListCard("🌿", ui(language, "recommendations"), result.recommendations)
            if (result.safety.isNotBlank()) SafetyCard(result.safety)
            Button(onClick = onVoice, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("🔊 " + tx(language, "Hear advice", "পরামর্শ শুনুন", "सलाह सुनें", "ஆலோசனையை கேளுங்கள்", "ਸਲਾਹ ਸੁਣੋ", "సలహా వినండి")) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(tx(language, "Scan another crop", "আরেকটি ফসল স্ক্যান করুন", "दूसरी फसल स्कैन करें", "மற்றொரு பயிரை ஸ்கேன் செய்யுங்கள்", "ਹੋਰ ਫਸਲ ਸਕੈਨ ਕਰੋ", "మరో పంటను స్కాన్ చేయండి")) }
        } else {
            error?.let { ErrorCard(it, onRetry) }
        }
    }
}

@Composable
private fun AdvisoryInputScreen(language: String, crop: String, onCropChange: (String) -> Unit, cropName: String, onCropNameChange: (String) -> Unit, query: String, onQuery: (String) -> Unit, onAsk: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(ui(language, "advisory"), language, onBack)
        Spacer(Modifier.height(18.dp))
        Text(tx(language, "Ask your crop question", "আপনার ফসল সম্পর্কে প্রশ্ন করুন", "अपनी फसल के बारे में पूछें", "உங்கள் பயிர் பற்றி கேளுங்கள்", "ਆਪਣੀ ਫਸਲ ਬਾਰੇ ਪੁੱਛੋ", "మీ పంట గురించి అడగండి"), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(tx(language, "Explain what you see. OpenKrishi AI will suggest safe next steps.", "আপনি যা দেখছেন তা লিখুন। OpenKrishi AI নিরাপদ পরবর্তী পদক্ষেপ বলবে।", "जो दिख रहा है उसे लिखें। OpenKrishi AI सुरक्षित अगले कदम बताएगा।", "நீங்கள் காண்பதை எழுதுங்கள். OpenKrishi AI பாதுகாப்பான அடுத்த படிகளைச் சொல்லும்.", "ਜੋ ਤੁਸੀਂ ਦੇਖ ਰਹੇ ਹੋ ਉਹ ਲਿਖੋ। OpenKrishi AI ਸੁਰੱਖਿਅਤ ਅਗਲੇ ਕਦਮ ਦੱਸੇਗਾ।", "మీరు గమనించినదాన్ని రాయండి. OpenKrishi AI సురక్షితమైన తదుపరి చర్యలను సూచిస్తుంది."), color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(CROP_ITEMS) { item -> AssistChip(onClick = { onCropChange(item.id) }, label = { Text(item.label) }) } }
        Spacer(Modifier.height(10.dp))
        DropdownLike("Crop / variety", cropName, cropOptions(crop), onCropNameChange)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(18.dp), placeholder = { Text("Example: rice leaves are turning yellow…") })
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onAsk(query) }, enabled = query.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) { Text(tx(language, "Get AI advisory", "AI পরামর্শ নিন", "AI सलाह लें", "AI ஆலோசனை பெறுங்கள்", "AI ਸਲਾਹ ਲਓ", "AI సలహా పొందండి"), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun VoiceScreen(language: String, crop: String, onCropChange: (String) -> Unit, cropName: String, onCropNameChange: (String) -> Unit, onAsk: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val tts = remember { TextToSpeech(context) {} }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        listening = false
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (text.isNotBlank()) {
            transcript = text
            onAsk(text)
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognizer(context, language, launcher) else error = "Microphone permission is required for voice advice."
    }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(ui(language, "voice"), language, onBack)
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(CROP_ITEMS) { item -> AssistChip(onClick = { onCropChange(item.id) }, label = { Text(item.label) }) } }
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(Purple), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(128.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Text("🎙️", fontSize = 58.sp) }
                Spacer(Modifier.height(18.dp))
                Text(if (listening) tx(language, "Listening…", "শুনছি…", "सुन रहा हूँ…", "கேட்கிறேன்…", "ਸੁਣ ਰਿਹਾ ਹਾਂ…", "వింటున్నాను…") else tx(language, "Speak your crop problem", "ফসলের সমস্যা বলুন", "फसल की समस्या बताएं", "பயிர் பிரச்சினையைச் சொல்லுங்கள்", "ਫਸਲ ਦੀ ਸਮੱਸਿਆ ਦੱਸੋ", "పంట సమస్యను చెప్పండి"), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(tx(language, "Your question and AI answer stay in the selected language.", "আপনার প্রশ্ন ও AI উত্তর নির্বাচিত ভাষাতেই থাকবে।", "आपका सवाल और AI जवाब चुनी हुई भाषा में रहेगा।", "உங்கள் கேள்வியும் AI பதிலும் தேர்ந்த மொழியிலேயே இருக்கும்.", "ਤੁਹਾਡਾ ਸਵਾਲ ਅਤੇ AI ਜਵਾਬ ਚੁਣੀ ਭਾਸ਼ਾ ਵਿੱਚ ਰਹੇਗਾ।", "మీ ప్రశ్న మరియు AI సమాధానం ఎంచుకున్న భాషలోనే ఉంటుంది."), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Button(onClick = {
                    if (!SpeechRecognizer.isRecognitionAvailable(context)) error = "Speech recognition is not available on this device."
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecognizer(context, language, launcher)
                    else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (listening) "● Listening" else "🎙️ " + tx(language, "Start speaking", "কথা বলা শুরু করুন", "बोलना शुरू करें", "பேசத் தொடங்குங்கள்", "ਬੋਲਣਾ ਸ਼ੁਰੂ ਕਰੋ", "మాట్లాడటం ప్రారంభించండి"))
                }
            }
        }
        transcript.takeIf { it.isNotBlank() }?.let { ResultCard("📝", tx(language, "Your speech", "আপনার কথা", "आपकी बात", "உங்கள் பேச்சு", "ਤੁਹਾਡੀ ਗੱਲ", "మీ మాట"), it) }
        error?.let { ErrorCard(it) {} }
    }
}

private fun startRecognizer(context: Context, language: String, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (language) {
            "বাংলা" -> "bn-IN"; "हिन्दी" -> "hi-IN"; "தமிழ்" -> "ta-IN"; "ਪੰਜਾਬੀ" -> "pa-IN"; "తెలుగు" -> "te-IN"; else -> "en-IN"
        })
        putExtra(RecognizerIntent.EXTRA_PROMPT, "OpenKrishi AI")
    }
    launcher.launch(intent)
}

@Composable
private fun WeatherScreen(language: String, weather: WeatherResult?, loading: Boolean, error: String?, onRefresh: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(ui(language, "weather"), language, onBack)
        Spacer(Modifier.height(14.dp))
        if (loading) LoadingCard(tx(language, "Loading local weather…", "স্থানীয় আবহাওয়া লোড হচ্ছে…", "स्थानीय मौसम लोड हो रहा है…", "உள்ளூர் வானிலை ஏற்றப்படுகிறது…", "ਸਥਾਨਕ ਮੌਸਮ ਲੋਡ ਹੋ ਰਿਹਾ ਹੈ…", "స్థానిక వాతావరణం లోడ్ అవుతోంది…"))
        weather?.let { w ->
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Sky), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("📍 " + w.location, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(w.temperature, color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(w.description, color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricChip("💧", w.humidity)
                        MetricChip("🌱", tx(language, "Farm-ready", "কৃষির জন্য প্রস্তুত", "खेती के लिए तैयार", "விவசாயத்திற்கு ஏற்றது", "ਖੇਤੀ ਲਈ ਤਿਆਰ", "వ్యవసాయానికి అనుకూలం"))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            ResultCard("⚠️", tx(language, "Farm advisory", "কৃষি পরামর্শ", "कृषि सलाह", "விவசாய ஆலோசனை", "ਖੇਤੀ ਸਲਾਹ", "వ్యవసాయ సలహా"), w.alert)
            Spacer(Modifier.height(10.dp))
            Text(tx(language, "7-day outlook", "৭ দিনের পূর্বাভাস", "7 दिन का पूर्वानुमान", "7 நாள் முன்னறிவிப்பு", "7 ਦਿਨਾਂ ਦੀ ਭਵਿੱਖਬਾਣੀ", "7 రోజుల అంచనా"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Today", "Tomorrow", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7")) { day ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.width(82.dp)) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day, fontSize = 10.sp, color = Muted)
                            Text(if (day == "Today") "☀️" else "🌤️", fontSize = 22.sp)
                            Text(if (day == "Today") w.temperature else "--", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        error?.let { Spacer(Modifier.height(10.dp)); ErrorCard(it) {} }
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text(tx(language, "Refresh weather", "আবহাওয়া আপডেট করুন", "मौसम अपडेट करें", "வானிலையை புதுப்பிக்கவும்", "ਮੌਸਮ ਤਾਜ਼ਾ ਕਰੋ", "వాతావరణాన్ని రిఫ్రెష్ చేయండి")) }
    }
}

@Composable
private fun HistoryScreen(modifier: Modifier, language: String, history: List<HistoryEntry>) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text(ui(language, "history"), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(tx(language, "Your recent OpenKrishi activity", "আপনার সাম্প্রতিক OpenKrishi কার্যকলাপ", "आपकी हाल की OpenKrishi गतिविधि", "உங்கள் சமீபத்திய OpenKrishi செயல்பாடுகள்", "ਤੁਹਾਡੀ ਹਾਲੀਆ OpenKrishi ਗਤੀਵਿਧੀ", "మీ ఇటీవలి OpenKrishi కార్యకలాపాలు"), color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        if (history.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕘", fontSize = 42.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(tx(language, "No activity yet", "এখনও কোনো কার্যকলাপ নেই", "अभी कोई गतिविधि नहीं", "இன்னும் செயல்பாடு இல்லை", "ਹਾਲੇ ਕੋਈ ਸਰਗਰਮੀ ਨਹੀਂ", "ఇంకా కార్యకలాపం లేదు"), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { entry ->
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.icon, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, color = Ink, fontWeight = FontWeight.Bold)
                                Text(entry.subtitle, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(modifier: Modifier, language: String, onLanguage: (String) -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text(ui(language, "profile"), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(62.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) { Text("👨‍🌾", fontSize = 32.sp) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(tx(language, "Farmer profile", "কৃষক প্রোফাইল", "किसान प्रोफ़ाइल", "விவசாயி சுயவிவரம்", "ਕਿਸਾਨ ਪ੍ਰੋਫਾਈਲ", "రైతు ప్రొఫైల్"), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("OpenKrishi AI", color = Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(tx(language, "App language", "অ্যাপের ভাষা", "ऐप भाषा", "பயன்பாட்டு மொழி", "ਐਪ ਭਾਸ਼ਾ", "యాప్ భాష"), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LANG_OPTIONS.forEach { option ->
                    Row(Modifier.fillMaxWidth().clickable { onLanguage(option) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = language == option, onClick = { onLanguage(option) })
                        Text(option, color = Ink, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        InfoCard("🛡️", tx(language, "Safe advisory", "নিরাপদ পরামর্শ", "सुरक्षित सलाह", "பாதுகாப்பான ஆலோசனை", "ਸੁਰੱਖਿਅਤ ਸਲਾਹ", "సురక్షిత సలహా"), tx(language, "OpenKrishi AI gives cautious guidance and encourages local agronomist confirmation for uncertain crop problems.", "অনিশ্চিত সমস্যায় স্থানীয় কৃষি বিশেষজ্ঞের পরামর্শ নিতে উৎসাহিত করা হয়।", "अनिश्चित समस्याओं में स्थानीय कृषि विशेषज्ञ से पुष्टि करने की सलाह दी जाती है।", "உறுதியற்ற பயிர் பிரச்சினைகளில் உள்ளூர் வேளாண் நிபுணரை அணுகவும்.", "ਅਣਸ਼ਚਿਤ ਫਸਲ ਸਮੱਸਿਆਵਾਂ ਵਿੱਚ ਸਥਾਨਕ ਖੇਤੀ ਮਾਹਿਰ ਨਾਲ ਪੁਸ਼ਟੀ ਕਰਨ ਦੀ ਸਲਾਹ ਦਿੱਤੀ ਜਾਂਦੀ ਹੈ।", "అనిశ్చిత పంట సమస్యల్లో స్థానిక వ్యవసాయ నిపుణుడిని సంప్రదించండి."))
        Spacer(Modifier.height(10.dp))
        Text("Version 0.1.0", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoCard(icon: String, title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Mint), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Text(icon, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column { Text(title, color = DeepGreen, fontWeight = FontWeight.Bold); Spacer(Modifier.height(3.dp)); Text(body, color = Muted, fontSize = 11.sp, lineHeight = 17.sp) }
        }
    }
}

@Composable
private fun ResultCard(icon: String, title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(body, color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ResultListCard(icon: String, title: String, values: List<String>) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(icon + "  " + title, color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            values.forEach { value ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = Green, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(7.dp))
                    Text(value, color = Ink, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SafetyCard(safety: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Amber), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Text("🛡️", fontSize = 20.sp)
            Spacer(Modifier.width(9.dp))
            Text(safety, color = Color(0xFF765000), fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun LoadingCard(text: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Green)
            Spacer(Modifier.height(14.dp))
            Text(text, color = Ink, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color(0xFFFFF0E8)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("Something needs attention", color = Color(0xFF8C3E16), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(message, color = Color(0xFF8C3E16), fontSize = 12.sp, lineHeight = 18.sp)
            TextButton(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
private fun MetricChip(icon: String, text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(.65f)) {
        Text(icon + " " + text, color = Ink, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
    }
}

@Composable
private fun TopBar(title: String, language: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("‹", fontSize = 38.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.width(6.dp))
        Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(50), color = Mint) {
            Text(language, color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
        }
    }
}

private fun cropOptions(crop: String): List<String> = when (crop) {
    "rice" -> RICE_VARIETIES
    "peanut" -> PEANUT_VARIETIES
    "vegetables" -> VEGETABLE_CROPS
    "flowers" -> FLOWER_CROPS
    else -> listOf("Other")
}

private fun cropIcon(crop: String): String = when (crop) {
    "rice" -> "🌾"
    "peanut" -> "🥜"
    "vegetables" -> "🥬"
    "flowers" -> "🌸"
    else -> "🌱"
}

private fun tx(language: String, en: String, bn: String, hi: String, ta: String, pa: String, te: String): String = when (language) {
    "বাংলা" -> bn
    "हिन्दी" -> hi
    "தமிழ்" -> ta
    "ਪੰਜਾਬੀ" -> pa
    "తెలుగు" -> te
    else -> en
}

private fun ui(language: String, key: String): String = when (key) {
    "home" -> tx(language, "Home", "হোম", "होम", "முகப்பு", "ਮੁੱਖ ਪੰਨਾ", "హోమ్")
    "history" -> tx(language, "History", "ইতিহাস", "इतिहास", "வரலாறு", "ਇਤਿਹਾਸ", "చరిత్ర")
    "profile" -> tx(language, "Profile", "প্রোফাইল", "प्रोफ़ाइल", "சுயவிவரம்", "ਪ੍ਰੋਫਾਈਲ", "ప్రొఫైల్")
    "diagnosis" -> tx(language, "Scan crop", "ফসল স্ক্যান", "फसल स्कैन", "பயிர் ஸ்கேன்", "ਫਸਲ ਸਕੈਨ", "పంట స్కాన్")
    "voice" -> tx(language, "Voice advice", "ভয়েস পরামর্শ", "वॉइस सलाह", "குரல் ஆலோசனை", "ਵੌਇਸ ਸਲਾਹ", "వాయిస్ సలహా")
    "weather" -> tx(language, "Weather", "আবহাওয়া", "मौसम", "வானிலை", "ਮੌਸਮ", "వాతావరణం")
    "advisory" -> tx(language, "AI advisory", "AI পরামর্শ", "AI सलाह", "AI ஆலோசனை", "AI ਸਲਾਹ", "AI సలహా")
    "observations" -> tx(language, "Observations", "পর্যবেক্ষণ", "निरीक्षण", "கவனிப்புகள்", "ਨਿਰੀਖਣ", "పరిశీలనలు")
    "possible_causes" -> tx(language, "Possible causes", "সম্ভাব্য কারণ", "संभावित कारण", "சாத்தியமான காரணங்கள்", "ਸੰਭਾਵਿਤ ਕਾਰਨ", "సంభావ్య కారణాలు")
    "recommendations" -> tx(language, "Recommendations", "পরামর্শ", "सुझाव", "பரிந்துரைகள்", "ਸਿਫ਼ਾਰਸ਼ਾਂ", "సిఫార్సులు")
    else -> key
}

private fun Context.contentResolverBitmap(uri: android.net.Uri): Bitmap? =
    runCatching { contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()
