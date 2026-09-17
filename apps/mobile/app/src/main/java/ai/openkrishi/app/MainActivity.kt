package ai.openkrishi.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun OpenKrishiApp() {
    MaterialTheme {
        Surface(color = Color(0xFFF6FAF7), modifier = Modifier.fillMaxSize()) {
            var tab by remember { mutableStateOf(AppTab.HOME) }
            var screen by remember { mutableStateOf("home") }
            var language by remember { mutableStateOf("বাংলা") }
            var selectedImage by remember { mutableStateOf<Bitmap?>(null) }

            val galleryLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let { selectedImage = contentResolverBitmap(it) }
                if (uri != null) screen = "diagnosis"
            }
            val cameraLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.TakePicturePreview()
            ) { bitmap ->
                if (bitmap != null) selectedImage = bitmap
                if (bitmap != null) screen = "diagnosis"
            }

            when (screen) {
                "diagnosis" -> DiagnosisScreen(
                    language = language,
                    selectedImage = selectedImage,
                    onBack = { screen = "home" },
                    onGallery = { galleryLauncher.launch("image/*") },
                    onCamera = { cameraLauncher.launch(null) },
                    onResult = { screen = "result" },
                    onVoice = { screen = "voice" }
                )
                "result" -> ResultScreen(
                    language = language,
                    selectedImage = selectedImage,
                    onBack = { screen = "diagnosis" },
                    onVoice = { screen = "voice" }
                )
                "voice" -> VoiceScreen(
                    language = language,
                    onBack = { screen = "home" }
                )
                else -> Scaffold(
                    containerColor = Color(0xFFF6FAF7),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            NavigationBarItem(
                                selected = tab == AppTab.HOME,
                                onClick = { tab = AppTab.HOME },
                                icon = { Text("⌂", fontSize = 24.sp) },
                                label = { Text("হোম") }
                            )
                            NavigationBarItem(
                                selected = tab == AppTab.HISTORY,
                                onClick = { tab = AppTab.HISTORY },
                                icon = { Text("◷", fontSize = 22.sp) },
                                label = { Text("ইতিহাস") }
                            )
                            NavigationBarItem(
                                selected = tab == AppTab.PROFILE,
                                onClick = { tab = AppTab.PROFILE },
                                icon = { Text("○", fontSize = 24.sp) },
                                label = { Text("প্রোফাইল") }
                            )
                        }
                    }
                ) { padding ->
                    when (tab) {
                        AppTab.HOME -> HomeScreen(
                            modifier = Modifier.padding(padding),
                            language = language,
                            onLanguage = { language = it },
                            onDiagnosis = { screen = "diagnosis" },
                            onVoice = { screen = "voice" }
                        )
                        AppTab.HISTORY -> HistoryScreen(Modifier.padding(padding))
                        AppTab.PROFILE -> ProfileScreen(Modifier.padding(padding), language)
                    }
                }
            }
        }
    }
}

private fun ComponentActivity.contentResolverBitmap(uri: android.net.Uri): Bitmap? =
    runCatching {
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

@Composable
private fun HomeScreen(
    modifier: Modifier,
    language: String,
    onLanguage: (String) -> Unit,
    onDiagnosis: () -> Unit,
    onVoice: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(305.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(KrishiDeep, Color(0xFF0B6B45), Color(0xFF65B76B))
                    )
                )
        ) {
            FieldPattern()
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark()
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("OpenKrishi AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Smart farming • Better tomorrow", color = Color.White.copy(.78f), fontSize = 12.sp)
                    }
                    LanguagePill(language, onLanguage)
                }
                Spacer(Modifier.height(34.dp))
                Text("নমস্কার, কৃষক বন্ধু!", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text(
                    "আপনার ফসলের সুস্থতা আমাদের লক্ষ্য।\nছবি তুলুন বা কথা বলুন — AI সাহায্য করবে।",
                    color = Color.White.copy(.88f), fontSize = 15.sp, lineHeight = 22.sp
                )
                Spacer(Modifier.height(18.dp))
                WeatherHeroCard()
            }
        }

        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            SectionTitle("আপনার জন্য দ্রুত সাহায্য", "সব টুল এক জায়গায়")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = "⌾",
                    title = "রোগ শনাক্তকরণ",
                    subtitle = "ছবি তুলে সমস্যা জানুন",
                    tint = KrishiMint,
                    iconTint = KrishiGreen,
                    onClick = onDiagnosis
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = "♩",
                    title = "ভয়েস পরামর্শ",
                    subtitle = "কথা বলুন, উত্তর শুনুন",
                    tint = KrishiPurple,
                    iconTint = Color(0xFF7046C8),
                    onClick = onVoice
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureCard(Modifier.weight(1f), "☁", "আবহাওয়া আপডেট", "বৃষ্টি ও সতর্কতা", KrishiSky, Color(0xFF2574C8)) { }
                FeatureCard(Modifier.weight(1f), "✦", "ফসলের পরামর্শ", "চাষের সঠিক গাইড", KrishiAmber, Color(0xFFC48200)) { }
            }
            Spacer(Modifier.height(22.dp))
            SectionTitle("জনপ্রিয় ফসল", "সব দেখুন →")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf("🌾\nধান", "🌿\nপাট", "🥜\nবাদাম", "🥬\nসবজি", "🌸\nফুল")) { crop ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.width(78.dp)
                    ) {
                        Text(crop, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            TrustStrip()
            Spacer(Modifier.height(12.dp))
            Text("Healthy Crops  •  Prosperous Farmers  •  A Greener Tomorrow", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.15f)),
        contentAlignment = Alignment.Center
    ) { Text("🌿", fontSize = 24.sp) }
}

@Composable
private fun LanguagePill(language: String, onLanguage: (String) -> Unit) {
    val next = if (language == "বাংলা") "हिन्दी" else "বাংলা"
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(.15f),
        modifier = Modifier.clickable { onLanguage(next) }
    ) {
        Text("文  $language", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
private fun WeatherHeroCard() {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(.97f), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🌤️", fontSize = 35.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("28°C", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("আংশিক মেঘলা", color = Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("⌖ Kolkata", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("82% আর্দ্রতা", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(action, color = KrishiGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier,
    icon: String,
    title: String,
    subtitle: String,
    tint: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(tint), contentAlignment = Alignment.Center) {
                Text(icon, color = iconTint, fontSize = 22.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun TrustStrip() {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(KrishiMint), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✓", color = KrishiGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("কৃষকের ভাষায়, কৃষকের পাশে", color = KrishiDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("AI Vision • Voice • Weather • ৬টি ভাষা", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FieldPattern() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0..7) {
            drawCircle(Color.White.copy(alpha = .035f), radius = 120f + i * 22f, center = Offset(w * .82f, h * .18f))
        }
        drawCircle(Color.White.copy(alpha = .08f), radius = 80f, center = Offset(w * .05f, h * .92f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosisScreen(
    language: String,
    selectedImage: Bitmap?,
    onBack: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onResult: () -> Unit,
    onVoice: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("রোগ শনাক্তকরণ", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", fontSize = 34.sp) } },
                actions = { Text("⚡ AI", color = KrishiGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF6FAF7)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(18.dp).verticalScroll(rememberScrollState())) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color(0xFF102019)), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(390.dp)) {
                    if (selectedImage != null) {
                        Image(selectedImage.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(.18f)))
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C4D37), Color(0xFF0B261B)))))
                        Text("🌿", fontSize = 72.sp, modifier = Modifier.align(Alignment.Center))
                    }
                    Column(Modifier.align(Alignment.BottomCenter).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(50), color = KrishiGreen.copy(.92f)) {
                            Text(if (selectedImage == null) "AI ছবি বিশ্লেষণ শুরু করুন" else "AI ছবি বিশ্লেষণের জন্য প্রস্তুত", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PickerButton("📷", "ক্যামেরা", Modifier.weight(1f), onCamera)
                PickerButton("▣", "গ্যালারি", Modifier.weight(1f), onGallery)
                PickerButton("▤", "ফাইল", Modifier.weight(1f), onGallery)
            }
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("ফসলের ধরন", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("🌾  ধান (Rice)", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = if (selectedImage != null) onResult else onCamera, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(KrishiGreen)) {
                Text(if (selectedImage == null) "📷  ছবি তুলুন" else "✦  AI দিয়ে বিশ্লেষণ করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onVoice, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("🎙  ভয়েসে সমস্যাটি বলুন", color = KrishiGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text("আপনার ভাষা: $language  •  ছবি পরিষ্কার ও কাছ থেকে তুলুন", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun PickerButton(icon: String, title: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(15.dp), color = Color.White, modifier = modifier.clickable(onClick = onClick).border(1.dp, Color(0xFFDCE7DF), RoundedCornerShape(15.dp))) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.height(5.dp))
            Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultScreen(language: String, selectedImage: Bitmap?, onBack: () -> Unit, onVoice: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("পরামর্শের ফলাফল", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", fontSize = 34.sp) } },
                actions = { Text("🔊", fontSize = 20.sp, modifier = Modifier.padding(end = 16.dp).clickable(onClick = onVoice)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF6FAF7)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(18.dp).verticalScroll(rememberScrollState())) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selectedImage != null) Image(selectedImage.asImageBitmap(), null, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                    else Box(Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(KrishiMint), contentAlignment = Alignment.Center) { Text("🌾", fontSize = 34.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ধান (Rice)", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Surface(shape = RoundedCornerShape(50), color = KrishiMint) { Text("সম্ভাব্য সমস্যা", color = KrishiGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) }
                        Text("বিশ্বাসযোগ্যতা  92%", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            InfoCard("⚠", "পাতা হলুদ হয়ে যাচ্ছে", "সম্ভাব্য পুষ্টির ঘাটতি / অতিরিক্ত পানি / শিকড়ের সমস্যা", KrishiAmber, Color(0xFF9A6A00))
            Spacer(Modifier.height(12.dp))
            Text("প্রস্তাবিত সমাধান", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            ActionItem("পুরনো ও নতুন পাতার রং তুলনা করুন")
            ActionItem("মাটির আর্দ্রতা ও পানি নিষ্কাশন পরীক্ষা করুন")
            ActionItem("পাতার নিচে পোকা বা রোগের দাগ দেখুন")
            ActionItem("কারণ নিশ্চিত না হওয়া পর্যন্ত অতিরিক্ত সার বা কীটনাশক ব্যবহার করবেন না")
            Spacer(Modifier.height(14.dp))
            InfoCard("🌱", "চারা পর্যায়: টিলারিং", "সুস্থ গাছের সঙ্গে আক্রান্ত গাছ তুলনা করুন এবং প্রয়োজনে স্থানীয় কৃষি বিশেষজ্ঞের পরামর্শ নিন।", KrishiMint, KrishiGreen)
            Spacer(Modifier.height(14.dp))
            WeatherMiniCard()
            Spacer(Modifier.height(14.dp))
            Button(onClick = onVoice, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(KrishiGreen)) {
                Text("🔊  এই পরামর্শ শুনুন", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("ভাষা: $language  •  AI পরামর্শ সহায়ক তথ্য; গুরুত্বপূর্ণ সিদ্ধান্তে স্থানীয় বিশেষজ্ঞের পরামর্শ নিন।", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun InfoCard(icon: String, title: String, body: String, background: Color, iconColor: Color) {
    Surface(shape = RoundedCornerShape(18.dp), color = background, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Text(icon, color = iconColor, fontSize = 21.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(body, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun ActionItem(text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text("✓", color = KrishiGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Ink, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun WeatherMiniCard() {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🌦️", fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("আবহাওয়া তথ্য", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("28°C  •  82% আর্দ্রতা  •  বৃষ্টি 10%", color = Muted, fontSize = 11.sp)
            }
            Text("›", color = KrishiGreen, fontSize = 25.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceScreen(language: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ভয়েস পরামর্শ", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", color = Color.White, fontSize = 34.sp) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KrishiDeep)
            )
        },
        containerColor = KrishiDeep
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(22.dp))
            Text("আপনার সমস্যা বলুন…", color = Color.White.copy(.82f), fontSize = 15.sp)
            Spacer(Modifier.height(26.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
                Box(Modifier.size(210.dp).clip(CircleShape).background(Color.White.copy(.05f)).border(1.dp, Color.White.copy(.12f), CircleShape))
                Box(Modifier.size(150.dp).clip(CircleShape).background(Color.White.copy(.09f)).border(1.dp, Color(0xFF76E7A0).copy(.45f), CircleShape))
                Box(Modifier.size(92.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF76E7A0), KrishiGreen))), contentAlignment = Alignment.Center) {
                    Text("🎙", fontSize = 40.sp)
                }
            }
            Spacer(Modifier.height(25.dp))
            Text("মাইক্রোফোন ধরে কথা বলুন", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("ভাষা: $language", color = Color.White.copy(.65f), fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
            Spacer(Modifier.height(35.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("উদাহরণ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("“আমার ধানের পাতায় হলুদ দাগ পড়েছে, কী করব?”", color = Ink, fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("AI আপনার ভাষায় উত্তর দেবে এবং উত্তরটি শুনিয়েও দেবে।", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(Color.White)) {
                Text("🎙  কথা বলা শুরু করুন", color = KrishiGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun HistoryScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("আপনার ইতিহাস", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("সাম্প্রতিক AI পরামর্শ", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
        Spacer(Modifier.height(18.dp))
        listOf("ধান — পাতা হলুদ", "ধান — পাতায় দাগ", "সবজি — গাছ ঢলে পড়া").forEach { item ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(KrishiMint), contentAlignment = Alignment.Center) { Text("🌿", fontSize = 24.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("AI advisory • সম্প্রতি", color = Muted, fontSize = 11.sp)
                    }
                    Text("›", color = KrishiGreen, fontSize = 25.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(modifier: Modifier, language: String) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("প্রোফাইল", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = KrishiMint, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(KrishiGreen), contentAlignment = Alignment.Center) { Text("🌾", fontSize = 29.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("কৃষক বন্ধু", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("OpenKrishi AI", color = Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        listOf("🌐  ভাষা: $language", "🔔  আবহাওয়া সতর্কতা", "🛡  গোপনীয়তা ও নিরাপত্তা", "ℹ  OpenKrishi AI সম্পর্কে").forEach { item ->
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(item, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
