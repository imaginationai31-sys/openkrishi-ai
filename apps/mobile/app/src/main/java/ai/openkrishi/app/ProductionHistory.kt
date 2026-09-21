package ai.openkrishi.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProductionHistoryScreen(
    modifier: Modifier,
    language: String,
    entries: List<HistoryEntry>,
    onClear: () -> Unit
) {
    val english = language == "English"
    Column(modifier.fillMaxSize().background(Color(0xFFF6FAF7)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if(english) "Your farm history" else tx(language,"Your farm history","আপনার কৃষি ইতিহাস","आपकी कृषि हिस्ट्री","உங்கள் விவசாய வரலாறு","ਤੁਹਾਡੀ ਖੇਤੀ ਹਿਸਟਰੀ","మీ వ్యవసాయ చరిత్ర"), color=Ink, fontSize=26.sp, fontWeight=FontWeight.Black)
                Text(if(english) "Past AI advice and crop checks, saved on this device." else tx(language,"Past AI advice and crop checks, saved on this device.","আগের AI পরামর্শ ও ফসল পরীক্ষা, এই ডিভাইসে সংরক্ষিত।","पिछली AI सलाह और फसल जांच इस डिवाइस पर सुरक्षित हैं।","முந்தைய AI ஆலோசனைகள் மற்றும் பயிர் சோதனைகள் இந்த சாதனத்தில் சேமிக்கப்பட்டுள்ளன.","ਪਿਛਲੀ AI ਸਲਾਹ ਅਤੇ ਫਸਲ ਜਾਂਚ ਇਸ ਡਿਵਾਈਸ 'ਤੇ ਸੁਰੱਖਿਅਤ ਹਨ।","మునుపటి AI సలహాలు మరియు పంట తనిఖీలు ఈ పరికరంలో భద్రపరచబడ్డాయి."), color=Muted, fontSize=12.sp)
            }
            if(entries.isNotEmpty()) TextButton(onClick=onClear) { Text(if(english)"Clear" else tx(language,"Clear","মুছুন","साफ़ करें","அழிக்கவும்","ਸਾਫ਼ ਕਰੋ","క్లియర్")) }
        }
        Spacer(Modifier.height(18.dp))
        if(entries.isEmpty()) {
            Card(shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(Color.White), modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("🌾",fontSize=48.sp); Spacer(Modifier.height(8.dp))
                    Text(if(english)"No history yet" else tx(language,"No history yet","এখনও কোনো ইতিহাস নেই","अभी कोई हिस्ट्री नहीं है","இன்னும் வரலாறு இல்லை","ਹਾਲੇ ਕੋਈ ਹਿਸਟਰੀ ਨਹੀਂ","ఇంకా చరిత్ర లేదు"), color=Ink,fontSize=18.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(if(english)"Scan a crop or ask OpenKrishi AI for advice. Your recent results will appear here." else tx(language,"Scan a crop or ask OpenKrishi AI for advice. Your recent results will appear here.","ফসল স্ক্যান করুন বা OpenKrishi AI-কে পরামর্শ জিজ্ঞাসা করুন। সাম্প্রতিক ফলাফল এখানে দেখা যাবে।","फसल स्कैन करें या OpenKrishi AI से सलाह पूछें। आपके हाल के परिणाम यहां दिखेंगे।","பயிரை ஸ்கேன் செய்யுங்கள் அல்லது OpenKrishi AI-யிடம் ஆலோசனை கேளுங்கள். சமீபத்திய முடிவுகள் இங்கே தோன்றும்.","ਫਸਲ ਸਕੈਨ ਕਰੋ ਜਾਂ OpenKrishi AI ਤੋਂ ਸਲਾਹ ਲਓ। ਤੁਹਾਡੇ ਹਾਲੀਆ ਨਤੀਜੇ ਇੱਥੇ ਦਿਖਾਈ ਦੇਣਗੇ।","పంటను స్కాన్ చేయండి లేదా OpenKrishi AIని అడగండి. మీ తాజా ఫలితాలు ఇక్కడ కనిపిస్తాయి."), color=Muted,fontSize=12.sp)
                }
            }
        } else {
            entries.forEach { e ->
                Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(Color.White), modifier=Modifier.fillMaxWidth().padding(bottom=10.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text(if(e.type=="Crop diagnosis")"📷" else "💬", fontSize=26.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.type,color=KrishiGreen,fontSize=12.sp,fontWeight=FontWeight.Bold)
                                Text(if(e.cropName.isBlank())e.crop.replaceFirstChar{it.uppercase()} else e.cropName,color=Ink,fontSize=17.sp,fontWeight=FontWeight.Black)
                            }
                            Text(e.timestamp,color=Muted,fontSize=10.sp)
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(e.summary,color=Muted,fontSize=12.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
