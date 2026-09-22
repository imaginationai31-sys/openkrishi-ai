package ai.openkrishi.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private enum class AppTab { HOME, HISTORY, PROFILE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenKrishiApp() }
    }
}

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
            var weatherLatitude by remember { mutableStateOf(22.5726) }
            var weatherLongitude by remember { mutableStateOf(88.3639) }
            var selectedCrop by remember { mutableStateOf("rice") }
            var selectedCropName by remember { mutableStateOf("") }
            var historyEntries by remember { mutableStateOf(loadHistory(context)) }
            var locationPermissionResult by remember { mutableStateOf<Boolean?>(null) }

            val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    selectedImage = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
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
            val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                locationPermissionResult = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            }
            fun openCamera() {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(null)
                } else permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            fun analyze() {
                analyzing = true
                advisoryResult = null
                advisoryError = null
                screen = "result"
                Thread {
                    try {
                        val result = OpenKrishiApi.assessImage(selectedImage ?: error("No crop image selected."), language, selectedCrop, cropName = selectedCropName)
                        Handler(Looper.getMainLooper()).post {
                            advisoryResult = result
                            historyEntries = addHistory(context, historyEntries, selectedCrop, selectedCropName, "Crop diagnosis", result.answer)
                            analyzing = false
                        }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            advisoryError = e.message ?: "OpenKrishi AI is unavailable."
                            analyzing = false
                        }
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
                        Handler(Looper.getMainLooper()).post {
                            advisoryResult = result
                            historyEntries = addHistory(context, historyEntries, selectedCrop, selectedCropName, "AI advice", result.answer)
                            analyzing = false
                        }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            advisoryError = e.message ?: "OpenKrishi AI is unavailable."
                            analyzing = false
                        }
                    }
                }.start()
            }
            fun loadWeather() {
                weatherLoading = true
                screen = "weather"
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    weatherLoading = false
                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    return
                }
                fetchCurrentLocation(context) { location ->
                    val lat = location?.latitude ?: weatherLatitude
                    val lon = location?.longitude ?: weatherLongitude
                    weatherLatitude = lat
                    weatherLongitude = lon
                    Thread {
                        try {
                            val result = OpenKrishiApi.getWeather(lat, lon, language)
                            Handler(Looper.getMainLooper()).post { weather = result; weatherLoading = false }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post {
                                advisoryError = e.message ?: "Weather service is unavailable."
                                weatherLoading = false
                            }
                        }
                    }.start()
                }
            }

            LaunchedEffect(locationPermissionResult) {
                if (locationPermissionResult == true) {
                    locationPermissionResult = null
                    loadWeather()
                }
            }

            when (screen) {
                "diagnosis" -> ProductionDiagnosisScreen(
                    language, selectedCrop, { selectedCrop = it; selectedCropName = "" },
                    selectedCropName, { selectedCropName = it }, selectedImage, imageSource, cameraDenied,
                    { screen = "home" }, { galleryLauncher.launch("image/*") }, ::openCamera,
                    { selectedImage = null; imageSource = null; advisoryResult = null; advisoryError = null },
                    ::analyze, { screen = "voice" }
                )
                "result" -> ProductionResultScreen(
                    selectedImage, selectedCrop, selectedCropName, language, advisoryResult, advisoryError,
                    analyzing, { screen = "diagnosis" }, ::analyze, { screen = "voice" }
                )
                "advisory" -> ProductionAdvisoryScreen(language, selectedCrop, selectedCropName, advisoryQuery, { advisoryQuery = it }, ::askAdvisory, analyzing, advisoryResult, advisoryError, { screen = "home" }, { askAdvisory(advisoryQuery) }, { screen = "voice" })
                "weather" -> ProductionWeatherScreen(language, weather, weatherLoading, ::loadWeather) { screen = "home" }
                "voice" -> ProductionVoiceScreen(language, selectedCrop, selectedCropName) { screen = "home" }
                else -> Scaffold(
                    containerColor = Color(0xFFF6FAF7),
                    bottomBar = {
                        NavigationBar(containerColor = Color.White) {
                            NavigationBarItem(tab == AppTab.HOME, { tab = AppTab.HOME }, icon = { Text("⌂") }, label = { Text(ui(language, "home")) })
                            NavigationBarItem(tab == AppTab.HISTORY, { tab = AppTab.HISTORY }, icon = { Text("◷") }, label = { Text(ui(language, "history")) })
                            NavigationBarItem(tab == AppTab.PROFILE, { tab = AppTab.PROFILE }, icon = { Text("○") }, label = { Text(ui(language, "profile")) })
                        }
                    }
                ) { padding ->
                    when (tab) {
                        AppTab.HOME -> HomeDashboard(Modifier.padding(padding), language, { language = it }, selectedCrop, selectedCropName, { crop, subcategory -> selectedCrop = crop; selectedCropName = subcategory }, { screen = "diagnosis" }, { screen = "voice" }, { loadWeather() }, { screen = "advisory" })
                        AppTab.HISTORY -> ProductionHistoryScreen(Modifier.padding(padding), language, historyEntries, {
                            historyEntries = emptyList()
                            saveHistory(context, historyEntries)
                        })
                        AppTab.PROFILE -> ProductionProfileScreen(Modifier.padding(padding), language, { language = it })
                    }
                }
            }
        }
    }
}

private fun fetchCurrentLocation(context: android.content.Context, onResult: (Location?) -> Unit) {
    val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { manager.isProviderEnabled(it) }
    var delivered = false
    fun deliver(location: Location?) {
        if (!delivered) {
            delivered = true
            onResult(location)
        }
    }
    val best = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    if (best != null) deliver(best)
    if (!delivered && providers.isEmpty()) {
        deliver(null)
        return
    }
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            runCatching { manager.removeUpdates(this) }
            deliver(location)
        }
    }
    providers.forEach { provider ->
        runCatching {
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }
    }
    Handler(Looper.getMainLooper()).postDelayed({
        runCatching { manager.removeUpdates(listener) }
        deliver(best)
    }, 5000L)
}

@Composable
private fun ProductionProfileScreen(modifier: Modifier, language: String, onLanguage: (String) -> Unit) {
    val options = listOf("English", "বাংলা", "हिन्दी", "தமிழ்", "ਪੰਜਾਬੀ", "తెలుగు")
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text(tx(language, "Profile & Settings", "প্রোফাইল ও সেটিংস", "प्रोफ़ाइल और सेटिंग्स", "சுயவிவரம் மற்றும் அமைப்புகள்", "ਪ੍ਰੋਫਾਈਲ ਅਤੇ ਸੈਟਿੰਗਾਂ", "ప్రొఫైల్ మరియు సెట్టింగ్స్"), color = Ink, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
            Text("🌾  OpenKrishi AI", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(tx(language, "AI farming advice in your language.", "আপনার ভাষায় AI কৃষি পরামর্শ।", "आपकी भाषा में AI कृषि सलाह।", "உங்கள் மொழியில் AI விவசாய ஆலோசனை.", "ਤੁਹਾਡੀ ਭਾਸ਼ਾ ਵਿੱਚ AI ਖੇਤੀ ਸਲਾਹ।", "మీ భాషలో AI వ్యవసాయ సలహా."), color = Muted)
        }}
        Spacer(Modifier.height(16.dp))
        Text(tx(language, "App language", "অ্যাপের ভাষা", "ऐप की भाषा", "பயன்பாட்டு மொழி", "ਐਪ ਦੀ ਭਾਸ਼ਾ", "యాప్ భాష"), style = MaterialTheme.typography.titleMedium)
        options.forEach { label ->
            Row(Modifier.fillMaxWidth()) {
                RadioButton(selected = language == label, onClick = { onLanguage(label) })
                Text(label, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) { Text(
            tx(language, "AI guidance is advisory. Verify important crop-treatment decisions with local agricultural experts.",
                "AI পরামর্শ নির্দেশনামূলক। গুরুত্বপূর্ণ ফসল চিকিৎসার সিদ্ধান্ত স্থানীয় কৃষি বিশেষজ্ঞের সঙ্গে যাচাই করুন।",
                "AI सलाह मार्गदर्शन है। महत्वपूर्ण फसल उपचार निर्णय स्थानीय कृषि विशेषज्ञ से सत्यापित करें।",
                "AI ஆலோசனை வழிகாட்டுதலுக்காக மட்டுமே. முக்கிய பயிர் சிகிச்சை முடிவுகளை உள்ளூர் வேளாண் நிபுணருடன் சரிபார்க்கவும்.",
                "AI ਸਲਾਹ ਮਾਰਗਦਰਸ਼ਨ ਹੈ। ਮਹੱਤਵਪੂਰਨ ਫਸਲ ਇਲਾਜ ਫੈਸਲੇ ਸਥਾਨਕ ਖੇਤੀ ਮਾਹਿਰ ਨਾਲ ਜਾਂਚੋ।",
                "AI సలహా మార్గదర్శకం మాత్రమే. ముఖ్యమైన పంట చికిత్స నిర్ణయాలను స్థానిక వ్యవసాయ నిపుణులతో నిర్ధారించండి."
            ), color = Muted, modifier = Modifier.padding(16.dp))
        }
    }
}
