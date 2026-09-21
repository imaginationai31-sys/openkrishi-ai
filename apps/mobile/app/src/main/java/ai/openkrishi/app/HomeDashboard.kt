package ai.openkrishi.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HomeGreen = Color(0xFF087443)
private val HomeDeep = Color(0xFF063D2A)
private val HomeMint = Color(0xFFE6F6EC)
private val HomeInk = Color(0xFF13231B)
private val HomeMuted = Color(0xFF66736B)

@Composable
fun HomeDashboard(
    modifier: Modifier,
    language: String,
    onLanguage: (String) -> Unit,
    onDiagnosis: () -> Unit,
    onVoice: () -> Unit,
    onWeather: () -> Unit,
    onAdvice: () -> Unit
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF7FBF8))
    ) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(Color.White, Color(0xFFF0FAF4), Color(0xFFDDF3E5)))
            ).padding(horizontal = 18.dp, vertical = 17.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(HomeGreen),
                    contentAlignment = Alignment.Center
                ) { Text("🌿", fontSize = 30.sp) }

                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("OpenKrishi AI", color = HomeDeep, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Smart farming, in your language", color = HomeMuted, fontSize = 11.sp)
                }

                TextButton(onClick = { }) {
                    Text("🌐", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(13.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📍", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your farm location", color = HomeMuted, fontSize = 10.sp)
                        Text("Kolkata, West Bengal", color = HomeDeep, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("28°C", color = HomeInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text("Partly cloudy", color = HomeMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 14.dp, vertical = 15.dp)) {
            Text("How can we help your crop today?", color = HomeDeep, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("Choose a simple way to get farming help.", color = HomeMuted, fontSize = 12.sp)

            Spacer(Modifier.height(12.dp))

            HomeAction(
                modifier = Modifier.fillMaxWidth().height(126.dp),
                icon = "📷",
                title = "Scan Crop",
                subtitle = "Take a photo and check crop symptoms",
                colors = listOf(Color(0xFF63D72A), Color(0xFF087443)),
                onClick = onDiagnosis
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HomeAction(
                    modifier = Modifier.weight(1f).height(122.dp),
                    icon = "🎙️",
                    title = "Voice Advice",
                    subtitle = "Ask by speaking",
                    colors = listOf(Color(0xFFB968F4), Color(0xFF5633D9)),
                    onClick = onVoice
                )
                HomeAction(
                    modifier = Modifier.weight(1f).height(122.dp),
                    icon = "💡",
                    title = "Ask AI",
                    subtitle = "Describe your problem",
                    colors = listOf(Color(0xFFFFC928), Color(0xFFFF7415)),
                    onClick = onAdvice
                )
            }

            Spacer(Modifier.height(10.dp))
            HomeAction(
                modifier = Modifier.fillMaxWidth().height(94.dp),
                icon = "☀️",
                title = "Weather & Farm Alerts",
                subtitle = "Local forecast and farming alerts",
                colors = listOf(Color(0xFF22BDE8), Color(0xFF087CE5)),
                onClick = onWeather
            )

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your crops", color = HomeDeep, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("4 categories", color = HomeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(
                    listOf(
                        Triple("🌾", "Rice", Color(0xFFE7F6E8)),
                        Triple("🥜", "Peanut", Color(0xFFFFF0D2)),
                        Triple("🥬", "Vegetables", Color(0xFFEAF7D8)),
                        Triple("🌸", "Flowers", Color(0xFFF2E4FF))
                    )
                ) { crop ->
                    Card(
                        modifier = Modifier.width(142.dp).height(122.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(crop.third)
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(crop.first, fontSize = 42.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(crop.second, color = HomeInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("→", color = HomeGreen, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(HomeMint)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 25.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Safe AI guidance", color = HomeDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("OpenKrishi gives cautious next steps and recommends local confirmation for uncertain crop problems.", color = HomeMuted, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Language: $language",
                color = HomeMuted,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun HomeAction(
    modifier: Modifier,
    icon: String,
    title: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(Color.Transparent),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(colors)).padding(15.dp)
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text(icon, fontSize = 30.sp)
                Spacer(Modifier.height(5.dp))
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = Color.White.copy(alpha = .92f), fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2)
            }
            Text(
                "→",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
