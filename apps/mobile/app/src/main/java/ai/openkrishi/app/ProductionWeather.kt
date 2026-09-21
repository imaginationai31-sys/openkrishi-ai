package ai.openkrishi.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductionWeatherScreen(language: String, weather: WeatherResult?, loading: Boolean, onRefresh: () -> Unit, onBack: () -> Unit) {
    val english = language == "English"
    val title = if (english) "Weather & Farm Alerts" else tx(language, "Weather & Farm Alerts", "আবহাওয়া ও কৃষি সতর্কতা", "मौसम और कृषि चेतावनी", "வானிலை மற்றும் விவசாய எச்சரிக்கைகள்", "ਮੌਸਮ ਅਤੇ ਖੇਤੀ ਚੇਤਾਵਨੀਆਂ", "వాతావరణం మరియు వ్యవసాయ హెచ్చరికలు")
    Column(Modifier.fillMaxSize().background(Color(0xFFF6FAF7)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        TopBar(title, language, onBack)
        Spacer(Modifier.height(14.dp))
        Card(shape=RoundedCornerShape(26.dp), colors=CardDefaults.cardColors(Color.Transparent), modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF0AAE8A), Color(0xFF087CE5)))).padding(20.dp)) {
                Text(if (english) "Today on your farm" else tx(language, "Today on your farm", "আজ আপনার খামারে", "आज आपके खेत में", "இன்று உங்கள் வயலில்", "ਅੱਜ ਤੁਹਾਡੇ ਖੇਤ ਵਿੱਚ", "ఈ రోజు మీ పొలంలో"), color=Color.White.copy(.9f), fontSize=13.sp, fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("🌤️", fontSize=48.sp); Spacer(Modifier.weight(1f))
                    if (loading) CircularProgressIndicator(color=Color.White) else Column(horizontalAlignment=Alignment.End) {
                        Text(weather?.temperature ?: "--°C", color=Color.White, fontSize=38.sp, fontWeight=FontWeight.Black)
                        Text(weather?.description ?: if (english) "Weather unavailable" else tx(language,"Weather unavailable","আবহাওয়ার তথ্য নেই","मौसम की जानकारी उपलब्ध नहीं","வானிலை தகவல் இல்லை","ਮੌਸਮ ਦੀ ਜਾਣਕਾਰੀ ਉਪਲਬਧ ਨਹੀਂ","వాతావరణ సమాచారం అందుబాటులో లేదు"), color=Color.White.copy(.9f), fontSize=12.sp, textAlign=TextAlign.End)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(weather?.location ?: if (english) "Your farm location" else tx(language,"Your farm location","আপনার খামারের অবস্থান","आपके खेत का स्थान","உங்கள் வயல் இருப்பிடம்","ਤੁਹਾਡੇ ਖੇਤ ਦੀ ਸਥਿਤੀ","మీ పొలం స్థానం"), color=Color.White.copy(.9f), fontSize=12.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp), modifier=Modifier.fillMaxWidth()) {
            WeatherMetricCard("💧", if(english)"Humidity" else tx(language,"Humidity","আর্দ্রতা","नमी","ஈரப்பதம்","ਨਮੀ","తేమ"), weather?.humidity ?: "--", Modifier.weight(1f))
            WeatherMetricCard("🌧️", if(english)"Rain" else tx(language,"Rain","বৃষ্টি","बारिश","மழை","ਮੀਂਹ","వర్షం"), if(loading)"--" else "Check forecast", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Text(if(english)"Farm alert" else tx(language,"Farm alert","কৃষি সতর্কতা","कृषि चेतावनी","விவசாய எச்சரிக்கை","ਖੇਤੀ ਚੇਤਾਵਨੀ","వ్యవసాయ హెచ్చరిక"), color=Ink, fontSize=19.sp, fontWeight=FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(Color(0xFFFFF4D8)), modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment=Alignment.Top) {
                Text("⚠️", fontSize=25.sp); Spacer(Modifier.height(1.dp))
                Text(weather?.alert ?: if(english)"Refresh to check current farm conditions." else tx(language,"Refresh to check current farm conditions.","বর্তমান কৃষি পরিস্থিতি জানতে আপডেট করুন।","वर्तमान कृषि स्थिति जानने के लिए अपडेट करें।","தற்போதைய விவசாய நிலையை அறிய புதுப்பிக்கவும்.","ਮੌਜੂਦਾ ਖੇਤੀ ਸਥਿਤੀ ਵੇਖਣ ਲਈ ਅੱਪਡੇਟ ਕਰੋ.","ప్రస్తుత వ్యవసాయ పరిస్థితిని తెలుసుకోవడానికి రిఫ్రెష్ చేయండి."), color=Color(0xFF6E4A00), fontSize=13.sp, lineHeight=19.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(if(english)"Farming reminder" else tx(language,"Farming reminder","কৃষি পরামর্শ","कृषि अनुस्मारक","விவசாய நினைவூட்டல்","ਖੇਤੀ ਯਾਦ","వ్యవసాయ గుర్తుచేయింపు"), color=Ink, fontSize=19.sp, fontWeight=FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(Color.White), modifier=Modifier.fillMaxWidth()) {
            Text(if(english)"Use weather information together with crop stage and field conditions before making irrigation or plant-protection decisions." else tx(language,"Use weather information together with crop stage and field conditions before making irrigation or plant-protection decisions.","সেচ বা গাছের সুরক্ষা সংক্রান্ত সিদ্ধান্তের আগে ফসলের পর্যায় ও জমির অবস্থার সঙ্গে আবহাওয়ার তথ্য মিলিয়ে দেখুন।","सिंचाई या पौध संरक्षण का निर्णय लेने से पहले फसल की अवस्था और खेत की स्थिति के साथ मौसम की जानकारी देखें।","பாசனம் அல்லது தாவர பாதுகாப்பு முடிவுகளுக்கு முன் பயிர் நிலை மற்றும் வயல் நிலவரத்துடன் வானிலை தகவலை இணைத்து பாருங்கள்.","ਸਿੰਚਾਈ ਜਾਂ ਪੌਧਾ ਸੁਰੱਖਿਆ ਦੇ ਫੈਸਲੇ ਤੋਂ ਪਹਿਲਾਂ ਫਸਲ ਦੀ ਅਵਸਥਾ ਅਤੇ ਖੇਤ ਦੀ ਸਥਿਤੀ ਨਾਲ ਮੌਸਮ ਦੀ ਜਾਣਕਾਰੀ ਮਿਲਾਓ।","నీటిపారుదల లేదా మొక్కల రక్షణ నిర్ణయాల ముందు పంట దశ మరియు పొలం పరిస్థితులతో వాతావరణ సమాచారాన్ని కలిపి చూడండి."), color=Muted, fontSize=13.sp, lineHeight=19.sp, modifier=Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick=onRefresh, modifier=Modifier.fillMaxWidth().height(54.dp), shape=RoundedCornerShape(17.dp), colors=ButtonDefaults.buttonColors(KrishiGreen)) {
            Text(if(english)"↻ Refresh weather" else tx(language,"↻ Refresh weather","↻ আবহাওয়া আপডেট করুন","↻ मौसम अपडेट करें","↻ வானிலை புதுப்பிக்கவும்","↻ ਮੌਸਮ ਅਪਡੇਟ ਕਰੋ","↻ వాతావరణాన్ని రిఫ్రెష్ చేయండి"), fontSize=15.sp, fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(if(english)"Weather data is advisory only. Check local field conditions before acting." else tx(language,"Weather data is advisory only. Check local field conditions before acting.","আবহাওয়ার তথ্য শুধু পরামর্শের জন্য। সিদ্ধান্তের আগে জমির স্থানীয় অবস্থা দেখুন।","मौसम की जानकारी केवल सलाह के लिए है। निर्णय से पहले खेत की स्थानीय स्थिति देखें।","வானிலை தகவல் ஆலோசனைக்காக மட்டுமே. முடிவு எடுக்கும் முன் உள்ளூர் வயல் நிலையை சரிபார்க்கவும்.","ਮੌਸਮ ਦੀ ਜਾਣਕਾਰੀ ਸਿਰਫ਼ ਸਲਾਹ ਲਈ ਹੈ। ਫੈਸਲਾ ਕਰਨ ਤੋਂ ਪਹਿਲਾਂ ਖੇਤ ਦੀ ਸਥਾਨਕ ਸਥਿਤੀ ਵੇਖੋ।","వాతావరణ సమాచారం సలహా కోసం మాత్రమే. నిర్ణయం ముందు స్థానిక పొలం పరిస్థితిని పరిశీలించండి."), color=Muted, fontSize=11.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
    }
}

@Composable
private fun WeatherMetricCard(icon:String,title:String,value:String,modifier:Modifier) {
    Card(modifier=modifier, shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(14.dp)) {
            Text(icon,fontSize=24.sp); Spacer(Modifier.height(6.dp)); Text(title,color=Muted,fontSize=11.sp,fontWeight=FontWeight.SemiBold); Spacer(Modifier.height(2.dp)); Text(value,color=Ink,fontSize=15.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
private fun ForecastCard(day: DailyForecast, today: Boolean, language: String) {
    val english = language == "English"
    Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(if(today) Color(0xFFEAF7EF) else Color.White), modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(if(today && english) "Today" else day.date, color=KrishiGreen, fontSize=14.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f))
                Text("🌦️", fontSize=24.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.fillMaxWidth()) {
                SmallWeatherValue(if(english) "Temp" else "তাপমাত্রা", "${day.minTemp} / ${day.maxTemp}", Modifier.weight(1f))
                SmallWeatherValue(if(english) "Rain" else "বৃষ্টি", day.rainProbability, Modifier.weight(1f))
                SmallWeatherValue(if(english) "Wind" else "বাতাস", day.windKmh, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Text(if(english) "Rain amount: ${day.rainMm}" else "বৃষ্টির পরিমাণ: ${day.rainMm}", color=Muted, fontSize=11.sp)
        }
    }
}

@Composable
private fun SmallWeatherValue(title:String, value:String, modifier:Modifier) {
    Column(modifier.background(Color.White.copy(alpha=.75f), RoundedCornerShape(12.dp)).padding(9.dp)) {
        Text(title,color=Muted,fontSize=10.sp)
        Text(value,color=Ink,fontSize=12.sp,fontWeight=FontWeight.Bold)
    }
}
