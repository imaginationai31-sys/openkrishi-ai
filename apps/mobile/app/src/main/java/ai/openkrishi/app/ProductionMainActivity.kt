package ai.openkrishi.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EntryGreen = Color(0xFF087443)
private val EntryDeep = Color(0xFF063D2A)
private val EntryMint = Color(0xFFE6F6EC)
private val EntryInk = Color(0xFF13231B)

class ProductionMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenKrishiEntry() }
    }
}

@Composable
private fun OpenKrishiEntry() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("openkrishi", Context.MODE_PRIVATE) }
    var showSplash by remember { mutableStateOf(true) }
    var onboardingComplete by remember { mutableStateOf(prefs.getBoolean("onboarding_complete", false)) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1100)
        showSplash = false
    }

    when {
        showSplash -> SplashScreen()
        !onboardingComplete -> OnboardingFlow {
            prefs.edit().putBoolean("onboarding_complete", true).apply()
            onboardingComplete = true
        }
        else -> OpenKrishiApp()
    }
}

@Composable
private fun SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, Color(0xFFF2FAF5), Color(0xFFDDF2E4))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(118.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("🌿", fontSize = 62.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("OpenKrishi AI", color = EntryDeep, fontSize = 31.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text("AI farming advice in your language", color = EntryGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(34.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(110.dp).clip(CircleShape),
                color = EntryGreen,
                trackColor = EntryMint
            )
        }
    }
}

@Composable
private fun OnboardingFlow(onComplete: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("🌾", "Welcome to OpenKrishi AI", "Get practical AI guidance for your crops, directly from your phone."),
        Triple("📷", "Scan • Ask • Listen", "Use camera, gallery or voice to describe crop problems and get easy-to-follow advice."),
        Triple("☀️", "Weather for Better Decisions", "See local weather, farming alerts and recommendations in one simple app.")
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FBF8))
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                if (page < pages.lastIndex) "Skip" else "",
                color = EntryGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        val item = pages[page]
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, EntryMint)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.first, fontSize = 82.sp)
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    item.second,
                    color = EntryDeep,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    item.third,
                    color = Color(0xFF5D7067),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.indices.forEach { index ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == page) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (index == page) EntryGreen else Color(0xFFB9CEC2))
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                if (page < pages.lastIndex) page++ else onComplete()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(EntryGreen)
        ) {
            Text(
                if (page < pages.lastIndex) "Continue" else "Get Started",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "English • বাংলা • हिन्दी • தமிழ் • ਪੰਜਾਬੀ • తెలుగు",
            color = Color(0xFF6B7B73),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
