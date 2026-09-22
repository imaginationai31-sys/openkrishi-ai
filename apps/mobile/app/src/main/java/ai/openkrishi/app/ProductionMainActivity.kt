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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

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
    var profileComplete by remember { mutableStateOf(prefs.getBoolean("profile_complete", false)) }
    var firebaseReady by remember { mutableStateOf(false) }
    val activity = context as ComponentActivity

    LaunchedEffect(Unit) {
        firebaseReady = runCatching { com.google.firebase.FirebaseApp.initializeApp(context) != null }.getOrDefault(false)
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1100)
        showSplash = false
    }

    when {
        showSplash -> SplashScreen()
        !firebaseReady -> FirebaseSetupScreen()
        FirebaseAuth.getInstance().currentUser == null -> PhoneRegistrationFlow(activity, prefs)
        !profileComplete -> ProfileNameScreen { name ->
            prefs.edit().putString("profile_name", name).putBoolean("profile_complete", true).apply()
            profileComplete = true
        }
        !onboardingComplete -> OnboardingFlow {
            prefs.edit().putBoolean("onboarding_complete", true).apply()
            onboardingComplete = true
        }
        else -> OpenKrishiApp()
    }
}

@Composable
private fun FirebaseSetupScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFFF7FBF8)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("OpenKrishi AI", color = EntryDeep, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text("Mobile verification is not configured yet.", color = EntryInk, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Connect this Android app to Firebase Phone Authentication before testing OTP registration.", color = Color(0xFF5D7067), fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PhoneRegistrationFlow(activity: ComponentActivity, prefs: android.content.SharedPreferences) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }

    fun sendOtp() {
        val normalized = phone.trim().replace(" ", "")
        val full = if (normalized.startsWith("+")) normalized else "+91$normalized"
        if (!Regex("^\\+?[1-9]\\d{9,14}$").matches(full)) { error = "Enter a valid mobile number."; return }
        error = ""; sending = true
        val auth = FirebaseAuth.getInstance()
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(full)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        sending = false
                        if (!task.isSuccessful) error = task.exception?.localizedMessage ?: "Verification failed." else otpSent = true
                    }
                }
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    sending = false
                    error = e.localizedMessage ?: "Could not send OTP."
                }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id; otpSent = true; sending = false
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        val id = verificationId ?: return
        if (code.length != 6) { error = "Enter the 6-digit OTP."; return }
        sending = true; error = ""
        val credential = PhoneAuthProvider.getCredential(id, code)
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
            sending = false
            if (!task.isSuccessful) error = task.exception?.localizedMessage ?: "Invalid OTP." else {
                prefs.edit().putString("verified_phone", FirebaseAuth.getInstance().currentUser?.phoneNumber ?: phone).apply()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF7FBF8)).padding(24.dp)) {
        Spacer(Modifier.height(30.dp))
        Text("🌱", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Create your farmer profile", color = EntryDeep, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(if (otpSent) "Enter the OTP sent to your mobile number." else "Register with your mobile number to keep your profile and farm history secure.", color = Color(0xFF5D7067), fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(26.dp))
        if (!otpSent) {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile number") }, placeholder = { Text("+91 98765 43210") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = ::sendOtp, enabled = !sending, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(EntryGreen)) { Text(if (sending) "Sending OTP…" else "Send OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        } else {
            OutlinedTextField(value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit OTP") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = ::verifyOtp, enabled = !sending, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(EntryGreen)) { Text(if (sending) "Verifying…" else "Verify & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            TextButton(onClick = { otpSent = false; verificationId = null; code = "" }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Change mobile number", color = EntryGreen) }
        }
        if (error.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(error, color = Color(0xFFB3261E), fontSize = 13.sp) }
        Spacer(Modifier.weight(1f))
        Text("By continuing, you agree to use your mobile number for account verification.", color = Color(0xFF718078), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProfileNameScreen(onComplete: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7FBF8)).padding(24.dp)) {
        Spacer(Modifier.height(40.dp))
        Text("👋", fontSize = 48.sp)
        Spacer(Modifier.height(14.dp))
        Text("What should we call you?", color = EntryDeep, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("Add your profile name. You can change it later from Profile.", color = Color(0xFF5D7067), fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(26.dp))
        OutlinedTextField(value = name, onValueChange = { if (it.length <= 80) name = it }, label = { Text("Profile name") }, placeholder = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Button(onClick = { if (name.trim().isNotEmpty()) onComplete(name.trim()) }, enabled = name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(EntryGreen)) { Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
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
