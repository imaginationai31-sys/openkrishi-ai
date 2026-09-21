package ai.openkrishi.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap

private fun dText(language:String, en:String, bn:String, hi:String, ta:String, pa:String, te:String) = when(language) {
    "বাংলা" -> bn; "हिन्दी" -> hi; "தமிழ்" -> ta; "ਪੰਜਾਬੀ" -> pa; "తెలుగు" -> te; else -> en
}

@Composable
internal fun ProductionDiagnosisScreen(
    language:String, selectedCrop:String, onCropChange:(String)->Unit,
    selectedCropName:String, onCropNameChange:(String)->Unit,
    selectedImage:Bitmap?, imageSource:ImageSource?, cameraDenied:Boolean,
    onBack:()->Unit, onGallery:()->Unit, onCamera:()->Unit, onClear:()->Unit,
    onAnalyze:()->Unit, onVoice:()->Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF6FAF7)).padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("‹", fontSize=36.sp, color=Ink, modifier=Modifier.clickable(onClick=onBack))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(dText(language,"Crop diagnosis","ফসলের বিশ্লেষণ","फसल विश्लेषण","பயிர் பகுப்பாய்வு","ਫਸਲ ਵਿਸ਼ਲੇਸ਼ਣ","పంట విశ్లేషణ"),fontSize=23.sp,fontWeight=FontWeight.Black,color=KrishiDeep)
                Text(dText(language,"Photo-based AI crop health check","ছবির মাধ্যমে AI ফসল স্বাস্থ্য পরীক্ষা","फोटो से AI फसल स्वास्थ्य जांच","படத்தின் மூலம் AI பயிர் ஆரோக்கியச் சோதனை","ਫੋਟੋ ਰਾਹੀਂ AI ਫਸਲ ਸਿਹਤ ਜਾਂਚ","ఫోటో ద్వారా AI పంట ఆరోగ్య పరీక్ష"),fontSize=11.sp,color=Muted)
            }
        }
        Spacer(Modifier.height(16.dp))

        if(selectedImage==null) {
            Card(Modifier.fillMaxWidth(),RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(Color.White),elevation=CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.size(86.dp).clip(RoundedCornerShape(24.dp)).background(KrishiMint),Alignment.Center){Text("🌿",fontSize=46.sp)}
                    Spacer(Modifier.height(13.dp))
                    Text(dText(language,"Add a clear crop photo","ফসলের একটি পরিষ্কার ছবি দিন","फसल की साफ तस्वीर जोड़ें","தெளிவான பயிர் படத்தைச் சேர்க்கவும்","ਫਸਲ ਦੀ ਸਾਫ਼ ਤਸਵੀਰ ਸ਼ਾਮਲ ਕਰੋ","స్పష్టమైన పంట ఫోటోను జోడించండి"),fontSize=19.sp,fontWeight=FontWeight.Bold,color=Ink,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(dText(language,"Capture the affected leaf, stem or fruit up close.","আক্রান্ত পাতা, কান্ড বা ফল কাছ থেকে তুলুন।","प्रभावित पत्ती, तने या फल की पास से तस्वीर लें।","பாதிக்கப்பட்ட இலை, தண்டு அல்லது பழத்தை அருகில் படமெடுக்கவும்.","ਪ੍ਰਭਾਵਿਤ ਪੱਤੇ, ਤਣੇ ਜਾਂ ਫਲ ਦੀ ਨੇੜੇ ਤੋਂ ਤਸਵੀਰ ਲਓ।","ప్రభావిత ఆకు, కాండం లేదా పండును దగ్గరగా ఫోటో తీయండి."),fontSize=12.sp,color=Muted,textAlign=TextAlign.Center,lineHeight=18.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(dText(language,"Choose image source","ছবির উৎস বেছে নিন","छवि स्रोत चुनें","பட மூலத்தைத் தேர்ந்தெடுக்கவும்","ਤਸਵੀਰ ਦਾ ਸਰੋਤ ਚੁਣੋ","చిత్ర మూలాన్ని ఎంచుకోండి"),fontSize=16.sp,fontWeight=FontWeight.Bold,color=Ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxWidth()) {
                DiagnosisSourceCard(Modifier.weight(1f),"📷",dText(language,"Camera","ক্যামেরা","कैमरा","கேமரா","ਕੈਮਰਾ","కెమెరా"),dText(language,"Take photo","ছবি তুলুন","फोटो लें","படம் எடுக்கவும்","ਤਸਵੀਰ ਲਓ","ఫోటో తీయండి"),KrishiMint,onCamera)
                DiagnosisSourceCard(Modifier.weight(1f),"🖼️",dText(language,"Gallery","গ্যালারি","गैलरी","கேலரி","ਗੈਲਰੀ","గ్యాలరీ"),dText(language,"Choose photo","ছবি বাছুন","फोटो चुनें","படத்தைத் தேர்ந்தெடுக்கவும்","ਤਸਵੀਰ ਚੁਣੋ","ఫోటో ఎంచుకోండి"),KrishiSky,onGallery)
            }
            if(cameraDenied){Spacer(Modifier.height(10.dp));Text(dText(language,"Camera permission was not granted. You can use Gallery instead.","ক্যামেরার অনুমতি দেওয়া হয়নি। গ্যালারি ব্যবহার করতে পারেন।","कैमरा अनुमति नहीं मिली। आप गैलरी का उपयोग कर सकते हैं।","கேமரா அனுமதி வழங்கப்படவில்லை. கேலரியைப் பயன்படுத்தலாம்.","ਕੈਮਰਾ ਇਜਾਜ਼ਤ ਨਹੀਂ ਮਿਲੀ। ਤੁਸੀਂ ਗੈਲਰੀ ਵਰਤ ਸਕਦੇ ਹੋ।","కెమెరా అనుమతి ఇవ్వబడలేదు. గ్యాలరీని ఉపయోగించవచ్చు."),color=Color(0xFF9A5B00),fontSize=11.sp)}
        } else {
            Card(Modifier.fillMaxWidth(),RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(Color.White),elevation=CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Image(selectedImage.asImageBitmap(),"Selected crop",Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(18.dp)),contentScale=ContentScale.Crop)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)){Text(dText(language,"Photo ready","ছবি প্রস্তুত","फोटो तैयार है","படம் தயார்","ਤਸਵੀਰ ਤਿਆਰ ਹੈ","ఫోటో సిద్ధంగా ఉంది"),color=KrishiGreen,fontWeight=FontWeight.Bold,fontSize=14.sp);Text(dText(language,"Tap change to select another photo","অন্য ছবি বাছতে পরিবর্তন করুন","दूसरी फोटो चुनने के लिए बदलें दबाएं","வேறு படத்தைத் தேர்ந்தெடுக்க மாற்று என்பதைத் தட்டவும்","ਹੋਰ ਤਸਵੀਰ ਲਈ ਬਦਲੋ ਦਬਾਓ","మరో ఫోటో కోసం మార్చు నొక్కండి"),color=Muted,fontSize=10.sp)}
                        Text(dText(language,"Change","পরিবর্তন","बदलें","மாற்று","ਬਦਲੋ","మార్చు"),color=KrishiGreen,fontWeight=FontWeight.Bold,fontSize=12.sp,modifier=Modifier.clickable(onClick=onClear))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick=onAnalyze,Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(KrishiGreen)){
                Text(dText(language,"Analyze with AI","AI দিয়ে বিশ্লেষণ করুন","AI से विश्लेषण करें","AI மூலம் பகுப்பாய்வு செய்யவும்","AI ਨਾਲ ਵਿਸ਼ਲੇਸ਼ਣ ਕਰੋ","AIతో విశ్లేషించండి"),fontSize=16.sp,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick=onVoice,Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(16.dp)){
                Text("🎙️ "+dText(language,"Get voice advisory","ভয়েস পরামর্শ নিন","वॉइस सलाह लें","குரல் ஆலோசனை பெறுங்கள்","ਵੌਇਸ ਸਲਾਹ ਲਓ","వాయిస్ సలహా పొందండి"))
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(),RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(KrishiMint)){
            Text("💡 "+dText(language,"Tip: use good light and show the affected area clearly for better analysis.","টিপস: ভালো আলোতে আক্রান্ত অংশটি পরিষ্কারভাবে তুলুন।","सुझाव: बेहतर विश्लेषण के लिए अच्छी रोशनी में प्रभावित हिस्से की साफ तस्वीर लें।","குறிப்பு: சிறந்த பகுப்பாய்வுக்கு நல்ல வெளிச்சத்தில் பாதிக்கப்பட்ட பகுதியை தெளிவாக படமெடுக்கவும்.","ਸੁਝਾਅ: ਵਧੀਆ ਵਿਸ਼ਲੇਸ਼ਣ ਲਈ ਚੰਗੀ ਰੌਸ਼ਨੀ ਵਿੱਚ ਪ੍ਰਭਾਵਿਤ ਹਿੱਸਾ ਸਾਫ਼ ਦਿਖਾਓ।","చిట్కా: మెరుగైన విశ్లేషణ కోసం మంచి వెలుతురులో ప్రభావిత భాగాన్ని స్పష్టంగా చూపండి."),color=KrishiDeep,fontSize=11.sp,lineHeight=17.sp,modifier=Modifier.padding(14.dp))
        }
    }
}

@Composable
private fun DiagnosisSourceCard(modifier:Modifier,icon:String,title:String,subtitle:String,tint:Color,onClick:()->Unit){
    Card(modifier.clickable(onClick=onClick),RoundedCornerShape(19.dp),colors=CardDefaults.cardColors(Color.White),elevation=CardDefaults.cardElevation(1.dp)){
        Column(Modifier.padding(vertical=18.dp,horizontal=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(tint),Alignment.Center){Text(icon,fontSize=27.sp)}
            Spacer(Modifier.height(8.dp));Text(title,fontSize=14.sp,fontWeight=FontWeight.Bold,color=Ink);Text(subtitle,fontSize=10.sp,color=Muted)
        }
    }
}

@Composable
internal fun ProductionResultScreen(selectedImage:Bitmap?,selectedCrop:String,selectedCropName:String,language:String,result:AdvisoryResult?,error:String?,analyzing:Boolean,onBack:()->Unit,onRetry:()->Unit,onVoice:()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF6FAF7)).padding(18.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Text("‹",fontSize=36.sp,color=Ink,modifier=Modifier.clickable(onClick=onBack));Spacer(Modifier.width(8.dp));Text(dText(language,"Diagnosis result","বিশ্লেষণের ফলাফল","विश्लेषण परिणाम","பகுப்பாய்வு முடிவு","ਵਿਸ਼ਲੇਸ਼ਣ ਨਤੀਜਾ","విశ్లేషణ ఫలితం"),fontSize=23.sp,fontWeight=FontWeight.Black,color=KrishiDeep)}
        Spacer(Modifier.height(14.dp))
        selectedImage?.let{Image(it.asImageBitmap(),"Crop result",Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(21.dp)),contentScale=ContentScale.Crop);Spacer(Modifier.height(12.dp))}
        when{
            analyzing->{Card(Modifier.fillMaxWidth(),RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator(color=KrishiGreen);Spacer(Modifier.height(14.dp));Text(dText(language,"Analyzing your crop…","আপনার ফসল বিশ্লেষণ করা হচ্ছে…","आपकी फसल का विश्लेषण हो रहा है…","உங்கள் பயிர் பகுப்பாய்வு செய்யப்படுகிறது…","ਤੁਹਾਡੀ ਫਸਲ ਦਾ ਵਿਸ਼ਲੇਸ਼ਣ ਹੋ ਰਿਹਾ ਹੈ…","మీ పంటను విశ్లేషిస్తున్నాము…"),fontWeight=FontWeight.Bold,color=Ink);Spacer(Modifier.height(5.dp));Text(dText(language,"Preparing advice in your selected language.","আপনার নির্বাচিত ভাষায় পরামর্শ প্রস্তুত হচ্ছে।","आपकी चुनी भाषा में सलाह तैयार की जा रही है।","நீங்கள் தேர்ந்தெடுத்த மொழியில் ஆலோசனை தயாராகிறது.","ਤੁਹਾਡੀ ਚੁਣੀ ਭਾਸ਼ਾ ਵਿੱਚ ਸਲਾਹ ਤਿਆਰ ਕੀਤੀ ਜਾ ਰਹੀ ਹੈ।","మీరు ఎంచుకున్న భాషలో సలహా సిద్ధమవుతోంది."),fontSize=11.sp,color=Muted,textAlign=TextAlign.Center)}}}
            error!=null->{DiagnosisResultCard("⚠️",dText(language,"Connection problem","সংযোগ সমস্যা","कनेक्शन समस्या","இணைப்பு சிக்கல்","ਕਨੈਕਸ਼ਨ ਸਮੱਸਿਆ","కనెక్షన్ సమస్య"),error);Spacer(Modifier.height(8.dp));Button(onClick=onRetry,Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(KrishiGreen)){Text(dText(language,"Try again","আবার চেষ্টা করুন","फिर कोशिश करें","மீண்டும் முயற்சிக்கவும்","ਦੁਬਾਰਾ ਕੋਸ਼ਿਸ਼ ਕਰੋ","మళ్లీ ప్రయత్నించండి"))}}
            result!=null->{DiagnosisResultCard("🌾",dText(language,"Crop","ফসল","फसल","பயிர்","ਫਸਲ","పంట"),cropLabel(selectedCrop,language)+(if(selectedCropName.isNotBlank())" • $selectedCropName" else ""));if(result.observations.isNotEmpty())DiagnosisResultCard("👁️",dText(language,"What AI observed","AI কী দেখেছে","AI ने क्या देखा","AI கவனித்தது","AI ਨੇ ਕੀ ਦੇਖਿਆ","AI గమనించినది"),result.observations.joinToString("\n• ","• "));if(result.possibleCauses.isNotEmpty())DiagnosisResultCard("🔎",dText(language,"Possible causes","সম্ভাব্য কারণ","संभावित कारण","சாத்தியமான காரணங்கள்","ਸੰਭਾਵਿਤ ਕਾਰਨ","సంభావ్య కారణాలు"),result.possibleCauses.joinToString("\n• ","• "));DiagnosisResultCard("💡",dText(language,"AI guidance","AI পরামর্শ","AI सलाह","AI ஆலோசனை","AI ਸਲਾਹ","AI సలహా"),result.answer);if(result.recommendations.isNotEmpty())DiagnosisResultCard("✓",dText(language,"Safe next steps","নিরাপদ পরবর্তী পদক্ষেপ","सुरक्षित अगले कदम","பாதுகாப்பான அடுத்த படிகள்","ਸੁਰੱਖਿਅਤ ਅਗਲੇ ਕਦਮ","సురక్షిత తదుపరి దశలు"),result.recommendations.joinToString("\n• ","• "));DiagnosisResultCard("◉",dText(language,"Confidence","আস্থা","विश्वास स्तर","நம்பிக்கை","ਭਰੋਸਾ","నమ్మకం"),result.confidence)}
            else->DiagnosisResultCard("ℹ️",dText(language,"Ready","প্রস্তুত","तैयार","தயார்","ਤਿਆਰ","సిద్ధంగా ఉంది"),dText(language,"Start an analysis to see the result.","বিশ্লেষণ শুরু করুন।","विश्लेषण शुरू करें।","பகுப்பாய்வைத் தொடங்கவும்.","ਵਿਸ਼ਲੇਸ਼ਣ ਸ਼ੁਰੂ ਕਰੋ।","విశ్లేషణ ప్రారంభించండి."))
        }
        Spacer(Modifier.height(10.dp))
        if(result!=null){Button(onClick=onVoice,Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(KrishiGreen)){Text("🔊 "+dText(language,"Listen to advice","পরামর্শ শুনুন","सलाह सुनें","ஆலோசனையைக் கேளுங்கள்","ਸਲਾਹ ਸੁਣੋ","సలహా వినండి"),fontWeight=FontWeight.Bold)}}
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(KrishiMint)){Text("🛡️ "+dText(language,"AI guidance is cautious. Confirm uncertain crop problems with a local agronomist.","AI পরামর্শ সতর্কভাবে দেওয়া হয়। অনিশ্চিত সমস্যায় স্থানীয় কৃষি বিশেষজ্ঞের পরামর্শ নিন।","AI सलाह सावधानी से दी जाती है। अनिश्चित समस्या में स्थानीय कृषि विशेषज्ञ से पुष्टि करें।","AI ஆலோசனை எச்சரிக்கையுடன் வழங்கப்படுகிறது. உறுதியற்ற பிரச்சினைகளில் உள்ளூர் வேளாண் நிபுணரை அணுகவும்.","AI ਸਲਾਹ ਸਾਵਧਾਨੀ ਨਾਲ ਦਿੱਤੀ ਜਾਂਦੀ ਹੈ। ਅਨਿਸ਼ਚਿਤ ਸਮੱਸਿਆ ਲਈ ਸਥਾਨਕ ਖੇਤੀ ਮਾਹਿਰ ਨਾਲ ਪੁਸ਼ਟੀ ਕਰੋ।","AI సలహా జాగ్రత్తగా ఇవ్వబడుతుంది. అనిశ్చిత సమస్యలలో స్థానిక వ్యవసాయ నిపుణుడిని సంప్రదించండి."),color=KrishiDeep,fontSize=10.sp,lineHeight=16.sp,modifier=Modifier.padding(13.dp))}
    }
}
@Composable private fun DiagnosisResultCard(icon:String,title:String,body:String){Card(Modifier.fillMaxWidth().padding(bottom=10.dp),RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(Color.White),elevation=CardDefaults.cardElevation(1.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.Top){Text(icon,fontSize=22.sp);Spacer(Modifier.width(10.dp));Column{Text(title,color=KrishiGreen,fontSize=12.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(4.dp));Text(body,color=Ink,fontSize=14.sp,lineHeight=20.sp)}}}}
