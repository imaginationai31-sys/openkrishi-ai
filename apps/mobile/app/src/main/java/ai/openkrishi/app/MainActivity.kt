package ai.openkrishi.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

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
        if (BuildConfig.DEBUG) {
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }
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
            var authUser by remember { mutableStateOf(FirebaseAuthManager.currentUser) }
            if (authUser == null) {
                LoginScreen(
                    onAuthenticated = { authUser = it }
                )
                return@Surface
            }
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
            var history by remember { mutableStateOf<List<DiagnosisHistoryItem>>(emptyList()) }
            var historyLoading by remember { mutableStateOf(false) }

            LaunchedEffect(authUser.uid) {
                FirestoreManager.saveFarmerProfile(authUser, authUser.displayName, language, selectedCrop, selectedCropName)
            }

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
                        Handler(Looper.getMainLooper()).post {
                            advisoryResult = result
                            analyzing = false
                            val image = selectedImage
                            if (image != null) {
                                FirebaseStorageManager.uploadCropImage(authUser, image) { upload ->
                                    val url = upload.getOrNull()
                                    FirestoreManager.saveDiagnosis(
                                        authUser, "vision", language, selectedCrop, selectedCropName,
                                        result = result, imageSource = imageSource?.name, imageUrl = url
                                    )
                                }
                            } else {
                                FirestoreManager.saveDiagnosis(
                                    authUser, "vision", language, selectedCrop, selectedCropName,
                                    result = result, imageSource = imageSource?.name
                                )
                            }
                        }
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
                        Handler(Looper.getMainLooper()).post {
                            advisoryResult = result
                            analyzing = false
                            FirestoreManager.saveDiagnosis(authUser, "advisory", language, selectedCrop, selectedCropName, query = query, result = result)
                        }
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
                "diagnosis" -> DiagnosisScreen(language, selectedCrop, { selectedCrop = it; selectedCropName = "" }, selectedCropName, { selectedCropName = it }, selectedImage, imageSource, cameraDenied, { screen = "home" }, { galleryLauncher.launch("image/*") }, ::openCamera, {
                    selectedImage = null; imageSource = null; advisoryResult = null; advisoryError = null
                }, ::analyze, { screen = "voice" })
                "result" -> ResultScreen(selectedImage, selectedCrop, selectedCropName, language, advisoryResult, advisoryError, analyzing, { screen = "diagnosis" }, ::analyze, { screen = "voice" })
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
                        AppTab.HOME -> HomeScreen(Modifier.padding(padding), language, { language = it }, { screen = "diagnosis" }, { screen = "voice" }, { screen = "weather"; loadWeather() }, { screen = "advisory" })
                        AppTab.HISTORY -> HistoryScreen(Modifier.padding(padding), language, authUser, history, historyLoading) {
                            historyLoading = true
                            FirestoreManager.loadHistory(authUser) {
                                it.onSuccess { items -> history = items }
                                    .onFailure { /* keep the existing history on transient errors */ }
                                historyLoading = false
                            }
                        }
                        AppTab.PROFILE -> ProfileScreen(Modifier.padding(padding), language, { newLanguage ->
                            language = newLanguage
                            FirestoreManager.saveFarmerProfile(authUser, authUser.displayName, newLanguage, selectedCrop, selectedCropName)
                        }) { FirebaseAuthManager.signOut(context); authUser = null }
                    }
                }
            }
        }
    }
}

