package com.wisal.app

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale
import java.util.concurrent.TimeUnit

private val AuthOrange = Color(0xFFE7A84B)
private val AuthBackground = Color(0xFF090D18)
private val AuthPanel = Color(0xFF151D2D)

private data class WisalCountry(val region: String, val name: String, val dialCode: Int) {
    val flag: String get() = region.uppercase(Locale.ROOT).map {
        String(Character.toChars(0x1F1E6 + (it.code - 'A'.code)))
    }.joinToString("")
}

private fun wisalCountries(): List<WisalCountry> {
    val phoneUtil = PhoneNumberUtil.getInstance()
    return Locale.getISOCountries().mapNotNull { region ->
        val dial = phoneUtil.getCountryCodeForRegion(region)
        if (dial <= 0) null else WisalCountry(
            region = region,
            name = Locale("ar", region).displayCountry.ifBlank { Locale("", region).displayCountry },
            dialCode = dial
        )
    }.distinctBy { it.region }.sortedBy { it.name }
}

@Composable
fun PhoneAuthScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }
    val countries = remember { wisalCountries() }
    var selectedCountry by remember { mutableStateOf(countries.firstOrNull { it.region == "TR" } ?: countries.first()) }
    var countryMenu by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var waitingForCode by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun finishSignIn(credential: PhoneAuthCredential) {
        busy = true
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            busy = false
            if (!task.isSuccessful) {
                message = task.exception?.localizedMessage ?: "تعذر التحقق من الرقم. حاول مرة أخرى."
            } else {
                message = "تم التحقق بنجاح."
                onSignedIn()
            }
        }
    }

    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) = finishSignIn(credential)
            override fun onVerificationFailed(e: FirebaseException) {
                busy = false
                message = e.localizedMessage ?: "فشل إرسال رمز التحقق."
            }
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                waitingForCode = true
                busy = false
                message = "أرسلنا رمز التحقق برسالة SMS."
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = AuthOrange, background = AuthBackground, surface = AuthPanel)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Box(Modifier.fillMaxSize().background(AuthBackground)) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.height(36.dp))
                    Text("W", color = AuthOrange, fontSize = 46.sp, fontWeight = FontWeight.Black)
                    Text("وِصال", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("رسائلك تقرّب المسافات", color = Color(0xFFB9C4D8), fontSize = 15.sp)
                    Spacer(Modifier.height(28.dp))
                    Surface(color = AuthPanel, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (waitingForCode) Icons.Default.Verified else Icons.Default.Phone, null, tint = AuthOrange, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(14.dp))
                            Text(if (waitingForCode) "تأكيد رقم الهاتف" else "مرحباً بك في وصال", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (waitingForCode) "أدخل رمز التحقق المكوّن من 6 أرقام" else "سجّل برقم هاتفك للتواصل مع جهات اتصالك",
                                color = Color(0xFFB9C4D8), textAlign = TextAlign.Center, fontSize = 13.sp
                            )
                            Spacer(Modifier.height(20.dp))
                            if (!waitingForCode) {
                                Box(Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { countryMenu = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text("${selectedCountry.flag}  ${selectedCountry.name}  +${selectedCountry.dialCode}", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                                        Text("⌄", color = AuthOrange, fontSize = 20.sp)
                                    }
                                    DropdownMenu(
                                        expanded = countryMenu,
                                        onDismissRequest = { countryMenu = false },
                                        modifier = Modifier.heightIn(max = 360.dp).background(AuthPanel)
                                    ) {
                                        countries.forEach { country ->
                                            DropdownMenuItem(
                                                text = { Text("${country.flag}  ${country.name}  +${country.dialCode}", color = Color.White) },
                                                onClick = { selectedCountry = country; countryMenu = false }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(15) },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    label = { Text("رقم الهاتف") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                        focusedBorderColor = AuthOrange, unfocusedBorderColor = Color(0xFF526078),
                                        focusedLabelColor = AuthOrange, cursorColor = AuthOrange
                                    )
                                )
                            } else {
                                OutlinedTextField(
                                    value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    label = { Text("رمز التحقق") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                        focusedBorderColor = AuthOrange, unfocusedBorderColor = Color(0xFF526078),
                                        focusedLabelColor = AuthOrange, cursorColor = AuthOrange
                                    )
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    message = ""
                                    if (waitingForCode) {
                                        val id = verificationId
                                        if (id == null || code.length != 6) message = "أدخل رمز التحقق المكوّن من 6 أرقام."
                                        else finishSignIn(PhoneAuthProvider.getCredential(id, code))
                                    } else {
                                        val local = phone.filter(Char::isDigit)
                                        if (activity == null) message = "تعذر بدء التحقق على هذا الجهاز."
                                        else if (local.length < 5) message = "أدخل رقم هاتف صحيحاً."
                                        else {
                                            busy = true
                                            val fullNumber = "+${selectedCountry.dialCode}$local"
                                            val options = PhoneAuthOptions.newBuilder(auth)
                                                .setPhoneNumber(fullNumber)
                                                .setTimeout(60L, TimeUnit.SECONDS)
                                                .setActivity(activity)
                                                .setCallbacks(callbacks)
                                                .build()
                                            PhoneAuthProvider.verifyPhoneNumber(options)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                enabled = !busy,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AuthOrange, contentColor = Color(0xFF101522))
                            ) {
                                if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                                else Text(if (waitingForCode) "تأكيد الرمز والدخول" else "متابعة", fontWeight = FontWeight.Bold)
                            }
                            if (waitingForCode) {
                                TextButton(onClick = { waitingForCode = false; verificationId = null; code = ""; message = "" }) {
                                    Text("تغيير رقم الهاتف", color = AuthOrange)
                                }
                            }
                            if (message.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(message, color = if (message.contains("نجاح")) AuthOrange else Color(0xFFFFB4AB), textAlign = TextAlign.Center, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("تواصل خاص مع عائلتك وأصدقائك", color = Color(0xFF7F8CA3), fontSize = 12.sp)
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
}
