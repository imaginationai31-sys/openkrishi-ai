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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                    Text(tx(language, "Smart farming, in your language", "আপনার ভাষায় স্মার্ট কৃষি", "आपकी भाषा में स्मार्ट खेती", "உங்கள் மொழியில் ஸ்மார்ட் விவசாயம்", "ਤੁਹਾਡੀ ਭਾਸ਼ਾ ਵਿੱਚ ਸਮਾਰਟ ਖੇਤੀ", "మీ భాషలో స్మార్ట్ వ్యవసాయం"), color = HomeMuted, fontSize = 11.sp)
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
                        Text(tx(language, "Crop categories", "ফসলের বিভাগ", "फसल श्रेणियाँ", "பயிர் வகைகள்", "ਫਸਲ ਸ਼੍ਰੇਣੀਆਂ", "పంట వర్గాలు"), modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = HomeDeep, fontSize = 14.sp, fontWeight = FontWeight.Black)
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
                            if (selectedCropName.isBlank()) tx(language, "Selected: ", "নির্বাচিত: ", "चयनित: ", "தேர்வு: ", "ਚੁਣਿਆ: ", "ఎంచుకున్న: ") + (cropMenuCategories.firstOrNull { it.id == selectedCrop }?.label ?: "Rice")
                            else tx(language, "Selected: ", "নির্বাচিত: ", "चयनित: ", "தேர்வு: ", "ਚੁਣਿਆ: ", "ఎంచుకున్న: ") + (cropMenuCategories.firstOrNull { it.id == selectedCrop }?.label ?: selectedCrop) + " • " + selectedCropName,
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
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tx(language, "Your farm location", "আপনার খামারের অবস্থান", "आपके खेत का स्थान", "உங்கள் பண்ணை இருப்பிடம்", "ਤੁਹਾਡੇ ਖੇਤ ਦੀ ਸਥਿਤੀ", "మీ పొలం స్థానం"), color = HomeMuted, fontSize = 10.sp)
                        Text(tx(language, "Current location", "বর্তমান অবস্থান", "वर्तमान स्थान", "தற்போதைய இருப்பிடம்", "ਮੌਜੂਦਾ ਸਥਿਤੀ", "ప్రస్తుత స్థానం"), color = HomeDeep, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("28°C", color = HomeInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(tx(language, "Partly cloudy", "আংশিক মেঘলা", "आंशिक बादल", "பகுதி மேகமூட்டம்", "ਅੰਸ਼ਿਕ ਬੱਦਲਵਾਈ", "పాక్షిక మేఘావృతం"), color = HomeMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 14.dp, vertical = 15.dp)) {
            Text(tx(language, "How can we help your crop today?", "আজ আপনার ফসলকে কীভাবে সাহায্য করতে পারি?", "आज आपकी फसल की कैसे मदद करें?", "இன்று உங்கள் பயிருக்கு எப்படி உதவலாம்?", "ਅੱਜ ਤੁਹਾਡੀ ਫਸਲ ਦੀ ਕਿਵੇਂ ਮਦਦ ਕਰੀਏ?", "ఈ రోజు మీ పంటకు ఎలా సహాయం చేయాలి?"), color = HomeDeep, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(tx(language, "Choose a simple way to get farming help.", "কৃষি সহায়তা পাওয়ার সহজ উপায় বেছে নিন।", "कृषि सहायता पाने का आसान तरीका चुनें।", "விவசாய உதவி பெற எளிய வழியைத் தேர்ந்தெடுக்கவும்.", "ਖੇਤੀ ਮਦਦ ਲਈ ਸੌਖਾ ਤਰੀਕਾ ਚੁਣੋ।", "వ్యవసాయ సహాయం పొందడానికి సులభమైన మార్గాన్ని ఎంచుకోండి।"), color = HomeMuted, fontSize = 12.sp)

            Spacer(Modifier.height(12.dp))
            HomeAction(Modifier.fillMaxWidth().height(126.dp), "📷", tx(language, "Scan Crop", "ফসল স্ক্যান", "फसल स्कैन", "பயிரை ஸ்கேன்", "ਫਸਲ ਸਕੈਨ", "పంటను స్కాన్"), tx(language, "Take a photo and check crop symptoms", "ছবি তুলে ফসলের লক্ষণ দেখুন", "फोटो लेकर फसल के लक्षण देखें", "புகைப்படம் எடுத்து பயிர் அறிகுறிகளைப் பாருங்கள்", "ਫੋਟੋ ਲੈ ਕੇ ਫਸਲ ਦੇ ਲੱਛਣ ਵੇਖੋ", "ఫోటో తీసి పంట లక్షణాలను చూడండి"), listOf(Color(0xFF63D72A), Color(0xFF087443)), onDiagnosis)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HomeAction(Modifier.weight(1f).height(122.dp), "🎙️", tx(language, "Voice Advice", "ভয়েস পরামর্শ", "वॉइस सलाह", "குரல் ஆலோசனை", "ਵੌਇਸ ਸਲਾਹ", "వాయిస్ సలహా"), tx(language, "Ask by speaking", "কথা বলে জিজ্ঞাসা করুন", "बोलकर पूछें", "பேசி கேளுங்கள்", "ਬੋਲ ਕੇ ਪੁੱਛੋ", "మాట్లాడి అడగండి"), listOf(Color(0xFFB968F4), Color(0xFF5633D9)), onVoice)
                HomeAction(Modifier.weight(1f).height(122.dp), "💡", tx(language, "Ask AI", "AI-কে জিজ্ঞাসা করুন", "AI से पूछें", "AI-யிடம் கேளுங்கள்", "AI ਨੂੰ ਪੁੱਛੋ", "AIని అడగండి"), tx(language, "Describe your problem", "আপনার সমস্যা বলুন", "अपनी समस्या बताएं", "உங்கள் பிரச்சினையைச் சொல்லுங்கள்", "ਆਪਣੀ ਸਮੱਸਿਆ ਦੱਸੋ", "మీ సమస్యను వివరించండి"), listOf(Color(0xFFFFC928), Color(0xFFFF7415)), onAdvice)
            }
            Spacer(Modifier.height(10.dp))
            HomeAction(Modifier.fillMaxWidth().height(94.dp), "☀️", tx(language, "Weather & Farm Alerts", "আবহাওয়া ও কৃষি সতর্কতা", "मौसम और कृषि चेतावनी", "வானிலை மற்றும் விவசாய எச்சரிக்கைகள்", "ਮੌਸਮ ਅਤੇ ਖੇਤੀ ਚੇਤਾਵਨੀਆਂ", "వాతావరణం మరియు వ్యవసాయ హెచ్చరికలు"), tx(language, "Local forecast and farming alerts", "স্থানীয় পূর্বাভাস ও কৃষি সতর্কতা", "स्थानीय पूर्वानुमान और कृषि चेतावनी", "உள்ளூர் முன்னறிவிப்பு மற்றும் விவசாய எச்சரிக்கைகள்", "ਸਥਾਨਕ ਪੂਰਵ-ਅਨੁਮਾਨ ਅਤੇ ਖੇਤੀ ਚੇਤਾਵਨੀਆਂ", "స్థానిక వాతావరణ అంచనా మరియు వ్యవసాయ హెచ్చరికలు"), listOf(Color(0xFF22BDE8), Color(0xFF087CE5)), onWeather)

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tx(language, "Your crops", "আপনার ফসল", "आपकी फसलें", "உங்கள் பயிர்கள்", "ਤੁਹਾਡੀਆਂ ਫਸਲਾਂ", "మీ పంటలు"), color = HomeDeep, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(tx(language, "4 categories", "৪টি বিভাগ", "4 श्रेणियाँ", "4 வகைகள்", "4 ਸ਼੍ਰੇਣੀਆਂ", "4 వర్గాలు"), color = HomeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(listOf(
                    Triple("🌾", tx(language, "Rice", "ধান", "चावल", "நெல்", "ਚੌਲ", "వరి"), Color(0xFFE7F6E8)),
                    Triple("🥜", tx(language, "Peanut", "চিনাবাদাম", "मूंगफली", "வேர்க்கடலை", "ਮੂੰਗਫਲੀ", "వేరుశెనగ"), Color(0xFFFFF0D2)),
                    Triple("🥬", tx(language, "Vegetables", "সবজি", "सब्ज़ियाँ", "காய்கறிகள்", "ਸਬਜ਼ੀਆਂ", "కూరగాయలు"), Color(0xFFEAF7D8)),
                    Triple("🌸", tx(language, "Flowers", "ফুল", "फूल", "மலர்கள்", "ਫੁੱਲ", "పూలు"), Color(0xFFF2E4FF))
                )) { crop ->
                    Card(Modifier.width(142.dp).height(122.dp), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(crop.third)) {
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
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
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(HomeMint)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 25.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(tx(language, "Safe AI guidance", "নিরাপদ AI পরামর্শ", "सुरक्षित AI मार्गदर्शन", "பாதுகாப்பான AI வழிகாட்டுதல்", "ਸੁਰੱਖਿਅਤ AI ਸਲਾਹ", "సురక్షిత AI మార్గదర్శకం"), color = HomeDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(tx(language, "OpenKrishi gives cautious next steps and recommends local confirmation for uncertain crop problems.", "OpenKrishi সতর্ক পরবর্তী পদক্ষেপ দেয় এবং অনিশ্চিত ফসল সমস্যায় স্থানীয়ভাবে নিশ্চিত করতে বলে।", "OpenKrishi सावधानी से अगले कदम बताता है और अनिश्चित फसल समस्याओं में स्थानीय पुष्टि की सलाह देता है।", "OpenKrishi எச்சரிக்கையான அடுத்த படிகளை வழங்குகிறது; உறுதியற்ற பயிர் பிரச்சினைகளில் உள்ளூர் நிபுணரிடம் உறுதி செய்ய பரிந்துரைக்கிறது.", "OpenKrishi ਸਾਵਧਾਨ ਅਗਲੇ ਕਦਮ ਦਿੰਦਾ ਹੈ ਅਤੇ ਅਣਸ਼ਚਿਤ ਫਸਲ ਸਮੱਸਿਆਵਾਂ ਲਈ ਸਥਾਨਕ ਪੁਸ਼ਟੀ ਦੀ ਸਲਾਹ ਦਿੰਦਾ ਹੈ।", "OpenKrishi జాగ్రత్తగా తదుపరి చర్యలను సూచిస్తుంది మరియు అనిశ్చిత పంట సమస్యలకు స్థానిక నిర్ధారణను సిఫార్సు చేస్తుంది."), color = HomeMuted, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(tx(language, "Language: ", "భాష: ", "भाषा: ", "மொழி: ", "ਭਾਸ਼ਾ: ", "భాష: ") + language, color = HomeMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun HomeAction(modifier: Modifier, icon: String, title: String, subtitle: String, colors: List<Color>, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(Color.Transparent), elevation = CardDefaults.cardElevation(4.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors)).padding(15.dp)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text(icon, fontSize = 30.sp)
                Spacer(Modifier.height(5.dp))
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = Color.White.copy(alpha = .92f), fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2)
            }
            Text("→", color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}
