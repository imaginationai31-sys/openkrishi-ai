package ai.openkrishi.app

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun vText(language:String,en:String,bn:String,hi:String,ta:String,pa:String,te:String)=when(language){"বাংলা"->bn;"हिन्दी"->hi;"தமிழ்"->ta;"ਪੰਜਾਬੀ"->pa;"తెలుగు"->te;else->en}

@Composable
fun ProductionVoiceScreen(language:String,selectedCrop:String,selectedCropName:String,onBack:()->Unit){
    val context=LocalContext.current
    var transcript by remember{mutableStateOf("")}
    var answer by remember{mutableStateOf("")}
    var listening by remember{mutableStateOf(false)}
    var loading by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    val tts=remember{TextToSpeech(context){}}
    DisposableEffect(Unit){onDispose{tts.stop();tts.shutdown()}}
    fun locale():Locale=when(language){"বাংলা"->Locale("bn","IN");"हिन्दी"->Locale("hi","IN");"தமிழ்"->Locale("ta","IN");"ਪੰਜਾਬੀ"->Locale("pa","IN");"తెలుగు"->Locale("te","IN");else->Locale.US}
    fun speak(text:String){tts.language=locale();tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"openkrishi")}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){r->
        listening=false
        val text=r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if(text.isNotBlank()){
            transcript=text; loading=true; error=null
            Thread{
                try{
                    val result=OpenKrishiApi.getAdvisory(text,language,selectedCrop,selectedCropName)
                    android.os.Handler(android.os.Looper.getMainLooper()).post{answer=result.answer;loading=false;speak(result.answer)}
                }catch(e:Exception){android.os.Handler(android.os.Looper.getMainLooper()).post{error=e.message?:vText(language,"Voice advisory is unavailable.","ভয়েস পরামর্শ পাওয়া যাচ্ছে না।","वॉइस सलाह उपलब्ध नहीं है।","குரல் ஆலோசனை கிடைக்கவில்லை.","ਵੌਇਸ ਸਲਾਹ ਉਪਲਬਧ ਨਹੀਂ ਹੈ।","వాయిస్ సలహా అందుబాటులో లేదు.");loading=false}}
            }.start()
        }
    }
    fun start(){
        if(!SpeechRecognizer.isRecognitionAvailable(context)){error=vText(language,"Speech recognition is unavailable on this device.","এই ডিভাইসে স্পিচ রিকগনিশন নেই।","इस डिवाइस पर स्पीच रिकग्निशन उपलब्ध नहीं है।","இந்த சாதனத்தில் பேச்சு அங்கீகாரம் இல்லை.","ਇਸ ਡਿਵਾਈਸ 'ਤੇ ਸਪੀਚ ਰਿਕਗਨਿਸ਼ਨ ਉਪਲਬਧ ਨਹੀਂ ਹੈ।","ఈ పరికరంలో స్పీచ్ రికగ్నిషన్ అందుబాటులో లేదు।");return}
        val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,when(language){"বাংলা"->"bn-IN";"हिन्दी"->"hi-IN";"தமிழ்"->"ta-IN";"ਪੰਜਾਬੀ"->"pa-IN";"తెలుగు"->"te-IN";else->"en-IN"})
            putExtra(RecognizerIntent.EXTRA_PROMPT,vText(language,"Speak your crop problem","আপনার ফসলের সমস্যা বলুন","अपनी फसल की समस्या बताएं","உங்கள் பயிர் பிரச்சினையைச் சொல்லுங்கள்","ਆਪਣੀ ਫਸਲ ਦੀ ਸਮੱਸਿਆ ਦੱਸੋ","మీ పంట సమస్యను చెప్పండి"))
        }
        listening=true;launcher.launch(i)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF6FAF7)).padding(18.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("‹",fontSize=36.sp,color=Ink,modifier=Modifier.clickable(onClick=onBack));Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)){Text(vText(language,"Voice advisory","ভয়েস পরামর্শ","वॉइस सलाह","குரல் ஆலோசனை","ਵੌਇਸ ਸਲਾਹ","వాయిస్ సలహా"),fontSize=23.sp,fontWeight=FontWeight.Black,color=KrishiDeep);Text(vText(language,"Speak naturally. Get guidance in the same language.","স্বাভাবিকভাবে বলুন। একই ভাষায় পরামর্শ পান।","स्वाभाविक रूप से बोलें। उसी भाषा में सलाह पाएं।","இயல்பாக பேசுங்கள். அதே மொழியில் ஆலோசனை பெறுங்கள்.","ਕੁਦਰਤੀ ਤੌਰ 'ਤੇ ਬੋਲੋ। ਉਸੇ ਭਾਸ਼ਾ ਵਿੱਚ ਸਲਾਹ ਲਓ।","సహజంగా మాట్లాడండి. అదే భాషలో సలహా పొందండి."),fontSize=11.sp,color=Muted)}
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(),RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(KrishiPurple)){
            Column(Modifier.padding(26.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Box(Modifier.size(108.dp).clip(RoundedCornerShape(54.dp)).background(Color(0xFFE4D6FF)),Alignment.Center){Text(if(listening) "🔴" else "🎙️",fontSize=48.sp)}
                Spacer(Modifier.height(14.dp))
                Text(if(listening)vText(language,"Listening…","শুনছি…","सुन रहा है…","கேட்கிறேன்…","ਸੁਣ ਰਿਹਾ ਹਾਂ…","వింటున్నాము…") else vText(language,"Tell us about your crop","আপনার ফসলের সমস্যা বলুন","अपनी फसल के बारे में बताएं","உங்கள் பயிரைப் பற்றி சொல்லுங்கள்","ਆਪਣੀ ਫਸਲ ਬਾਰੇ ਦੱਸੋ","మీ పంట గురించి చెప్పండి"),fontSize=20.sp,fontWeight=FontWeight.Black,color=Ink)
                Spacer(Modifier.height(7.dp))
                Text(vText(language,"Ask about yellow leaves, pests, watering, growth or any crop problem.","পাতা হলুদ হওয়া, পোকা, সেচ, বৃদ্ধি বা যেকোনো সমস্যা সম্পর্কে জিজ্ঞাসা করুন।","पीली पत्तियों, कीट, सिंचाई, बढ़वार या किसी भी समस्या के बारे में पूछें।","மஞ்சள் இலைகள், பூச்சிகள், நீர்ப்பாசனம், வளர்ச்சி அல்லது ஏதேனும் பயிர் பிரச்சினையை கேளுங்கள்.","ਪੀਲੇ ਪੱਤੇ, ਕੀੜੇ, ਸਿੰਚਾਈ, ਵਾਧੇ ਜਾਂ ਕਿਸੇ ਵੀ ਫਸਲ ਸਮੱਸਿਆ ਬਾਰੇ ਪੁੱਛੋ।","పసుపు ఆకులు, పురుగులు, నీరు, పెరుగుదల లేదా ఏదైనా పంట సమస్య గురించి అడగండి."),fontSize=12.sp,color=Muted,textAlign=TextAlign.Center,lineHeight=18.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick=::start,enabled=!listening&&!loading,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(KrishiGreen)){Text(if(listening)vText(language,"Listening","వింటున్నాము","सुन रहा है","கேட்கிறேன்","ਸੁਣ ਰਿਹਾ ਹਾਂ","వింటున్నాము") else "🎙️ "+vText(language,"Start speaking","কথা বলা শুরু করুন","बोलना शुरू करें","பேசத் தொடங்குங்கள்","ਬੋਲਣਾ ਸ਼ੁਰੂ ਕਰੋ","మాట్లాడటం ప్రారంభించండి"),fontWeight=FontWeight.Bold,fontSize=15.sp)}
            }
        }
        if(loading){Spacer(Modifier.height(16.dp));LinearProgressIndicator(Modifier.fillMaxWidth(),color=KrishiGreen)}
        if(transcript.isNotBlank()){Spacer(Modifier.height(16.dp));VoiceResultCard("📝",vText(language,"You said","আপনি বলেছেন","आपने कहा","நீங்கள் கூறியது","ਤੁਸੀਂ ਕਿਹਾ","మీరు చెప్పారు"),transcript)}
        if(answer.isNotBlank()){VoiceResultCard("💡",vText(language,"AI advice","AI পরামর্শ","AI सलाह","AI ஆலோசனை","AI ਸਲਾਹ","AI సలహా"),answer);OutlinedButton(onClick={speak(answer)},modifier=Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(15.dp)){Text("🔊 "+vText(language,"Listen again","আবার শুনুন","फिर सुनें","மீண்டும் கேளுங்கள்","ਦੁਬਾਰਾ ਸੁਣੋ","మళ్లీ వినండి"))}}
        error?.let{Spacer(Modifier.height(12.dp));Card(Modifier.fillMaxWidth(),RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(Color(0xFFFFF4D8))){Text("⚠️ $it",color=Color(0xFF7A4B00),fontSize=11.sp,modifier=Modifier.padding(13.dp))}}
        Spacer(Modifier.height(14.dp));Text(vText(language,"Your selected crop and language are used for the advisory.","আপনার নির্বাচিত ফসল ও ভাষা পরামর্শে ব্যবহার করা হয়।","आपकी चुनी फसल और भाषा सलाह में उपयोग की जाती है।","நீங்கள் தேர்ந்தெடுத்த பயிரும் மொழியும் ஆலோசனையில் பயன்படுத்தப்படும்.","ਤੁਹਾਡੀ ਚੁਣੀ ਫਸਲ ਅਤੇ ਭਾਸ਼ਾ ਸਲਾਹ ਵਿੱਚ ਵਰਤੀ ਜਾਂਦੀ ਹੈ।","మీరు ఎంచుకున్న పంట మరియు భాష సలహాలో ఉపయోగించబడతాయి."),color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
    }
}
@Composable private fun VoiceResultCard(icon:String,title:String,body:String){Card(Modifier.fillMaxWidth().padding(bottom=10.dp),RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(Color.White),elevation=CardDefaults.cardElevation(1.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.Top){Text(icon,fontSize=22.sp);Spacer(Modifier.width(10.dp));Column{Text(title,color=KrishiGreen,fontSize=12.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(4.dp));Text(body,color=Ink,fontSize=14.sp,lineHeight=20.sp)}}}}
