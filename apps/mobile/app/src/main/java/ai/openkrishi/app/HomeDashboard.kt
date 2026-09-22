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

private data class CropMenuCategory(val id: String, val label: String, val icon: String, val subcategories: List<String>)
private val cropMenuCategories = listOf(
    CropMenuCategory("rice", "Rice", "🌾", listOf("Basmati", "Sona Masuri", "IR64", "Other Rice")),
    CropMenuCategory("peanut", "Peanut", "🥜", listOf("Spanish", "Virginia", "Valencia", "Runner", "Other Peanut")),
    CropMenuCategory("vegetables", "Vegetables", "🥬", listOf("Brinjal", "Okra", "Potato", "Tomato", "Chilli", "Onion", "Cauliflower", "Cabbage", "Other Vegetable")),
    CropMenuCategory("flowers", "Flowers", "🌸", listOf("Rose", "Marigold", "Jasmine", "Chrysanthemum", "Gerbera", "Tuberose", "Other Flower"))
)

@Composable
fun HomeDashboard(
    modifier: Modifier,
    language: String,
    onLanguage: (String) -> Unit,
    selectedCrop: String,
    selectedCropName: String,
    onCropSelection: (String, String) -> Unit,
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

                var menuExpanded by remember { mutableStateOf(false) }
                var expandedCategory by remember { mutableStateOf<String?>(null) }

                Box {
                    IconButton(onClick = { menuExpanded = true; expandedCategory = null }) {
                        Text("☰", color = HomeDeep, fontSize = 25.sp)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false; expandedCategory = null }
                    ) {
                        Text("Crop categories", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = HomeDeep, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        HorizontalDivider()
                        cropMenuCategories.forEach { category ->
                            val isSelectedCategory = selectedCrop == category.id
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        expandedCategory = if (expandedCategory == category.id) null else category.id
                                    }.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(category.icon, fontSize = 20.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(category.label, modifier = Modifier.weight(1f), color = if (isSelectedCategory) HomeGreen else HomeInk, fontSize = 14.sp, fontWeight = if (isSelectedCategory) FontWeight.Bold else FontWeight.Medium)
                                    Text(if (expandedCategory == category.id) "⌃" else "⌄", color = HomeMuted, fontSize = 16.sp)
                                }
                                if (expandedCategory == category.id) {
                                    category.subcategories.forEach { subcategory ->
                                        val selected = isSelectedCategory && selectedCropName == subcategory
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                onCropSelection(category.id, subcategory)
                                                menuExpanded = false
                                                expandedCategory = null
                                            }.padding(start = 48.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (selected) "✓" else "•", color = if (selected) HomeGreen else HomeMuted, fontSize = 13.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Text(subcategory, color = if (selected) HomeGreen else HomeInk, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                        Text(
                            if (selectedCropName.isBlank()) "Selected: " + (cropMenuCategories.firstOrNull { it.id == selectedCrop }?.label ?: "Rice")
                            else "Selected: " + (cropMenuCategories.firstOrNull { it.id == selectedCrop }?.label ?: selectedCrop) + " • " + selectedCropName,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = HomeMuted, fontSize = 10.sp
                        )
                    }
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
