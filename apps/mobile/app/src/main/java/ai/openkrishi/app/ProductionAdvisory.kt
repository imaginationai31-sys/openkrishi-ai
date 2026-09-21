package ai.openkrishi.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductionAdvisoryScreen(language:String, crop:String, cropName:String, query:String, onQuery:(String)->Unit, onAsk:(String)->Unit, loading:Boolean, result:AdvisoryResult?, error:String?, onBack:()->Unit, onRetry:()->Unit, onVoice:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        TopBar(tx(language,"Crop advisory","ফসলের পরামর্শ","फसल सलाह","பயிர் ஆலோசனை","ਫਸਲ ਸਲਾਹ","పంట సలహా"),language,onBack)
        Spacer(Modifier.height(16.dp))
        Text(cropLabel(crop,language) + if(cropName.isBlank()) "" else " • $cropName", color=KrishiGreen, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value=query,onValueChange=onQuery,modifier=Modifier.fillMaxWidth().height(150.dp),placeholder={Text(tx(language,"Example: rice leaves are turning yellow","উদাহরণ: ধানের পাতা হলুদ হচ্ছে","उदाहरण: धान की पत्तियां पीली हो रही हैं","உதாரணம்: நெல் இலைகள் மஞ்சளாகின்றன","ਉਦਾਹਰਨ: ਝੋਨੇ ਦੇ ਪੱਤੇ ਪੀਲੇ ਹੋ ਰਹੇ ਹਨ","ఉదాహరణ: వరి ఆకులు పసుపు రంగులోకి మారుతున్నాయి"))})
        Spacer(Modifier.height(10.dp))
        Button(onClick={onAsk(query)},enabled=query.isNotBlank()&&!loading,modifier=Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(KrishiGreen)){Text(if(loading) tx(language,"Thinking…","ভাবছি…","सोच रहा है…","யோசிக்கிறேன்…","ਸੋਚ ਰਿਹਾ ਹਾਂ…","ఆలోచిస్తున్నాను…") else tx(language,"Get AI advisory","AI পরামর্শ নিন","AI सलाह लें","AI ஆலோசனை பெறுங்கள்","AI ਸਲਾਹ ਲਓ","AI సలహా పొందండి"))}
        Spacer(Modifier.height(14.dp))
        if(error!=null) Card(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(KrishiAmber)){Text("⚠️ $error",modifier=Modifier.padding(14.dp),color=Color(0xFF7A4B00),fontSize=12.sp)}
        result?.let { r ->
            Spacer(Modifier.height(10.dp))
            Card(Modifier.fillMaxWidth(),RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.padding(16.dp)){
                Text(tx(language,"AI guidance","AI পরামর্শ","AI सलाह","AI ஆலோசனை","AI ਸਲਾਹ","AI సలహా"),color=KrishiGreen,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(6.dp)); Text(r.answer,color=Ink,fontSize=14.sp,lineHeight=20.sp)
                if(r.recommendations.isNotEmpty()){Spacer(Modifier.height(10.dp));Text(r.recommendations.joinToString("\n• ","• "),color=Muted,fontSize=12.sp)}
            }}
            Spacer(Modifier.height(10.dp)); OutlinedButton(onClick=onVoice,modifier=Modifier.fillMaxWidth()){Text("🔊 "+tx(language,"Listen to advice","পরামর্শ শুনুন","सलाह सुनें","ஆலோசனையைக் கேளுங்கள்","ਸਲਾਹ ਸੁਣੋ","సలహా వినండి"))}
        }
    }
}
