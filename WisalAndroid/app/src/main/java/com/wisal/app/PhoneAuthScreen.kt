package com.wisal.app

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import java.util.concurrent.TimeUnit

private val AuthOrange = Color(0xFFFF8A00)
private val AuthBackground = Color(0xFF121212)
private val AuthPanel = Color(0xFF242424)

@Composable
fun PhoneAuthScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }
    var phone by remember { mutableStateOf("+90") }
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
            }
        }
    }

    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                finishSignIn(credential)
            }

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
            Column(
                Modifier.fillMaxSize().background(AuthBackground).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("وصال", color = AuthOrange, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("تواصل مع من تحب أينما كانوا", color = Color.LightGray, fontSize = 15.sp)
                Spacer(Modifier.height(30.dp))
                Surface(color = AuthPanel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (waitingForCode) Icons.Default.Verified else Icons.Default.Phone, null, tint = AuthOrange, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(14.dp))
                        Text(if (waitingForCode) "تأكيد رقم الهاتف" else "تسجيل الدخول برقم الهاتف", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (waitingForCode) "أدخل رمز التحقق الذي وصلك برسالة SMS" else "أدخل رقمك مع مفتاح الدولة، مثال: +905xxxxxxxxx",
                            color = Color.LightGray, textAlign = TextAlign.Center, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        if (!waitingForCode) {
                            OutlinedTextField(
                                value = phone, onValueChange = { phone = it },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                label = { Text("رقم الهاتف الدولي") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = AuthOrange, unfocusedBorderColor = Color.Gray,
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
                                    focusedBorderColor = AuthOrange, unfocusedBorderColor = Color.Gray,
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
                                    if (id == null || code.length < 6) {
                                        message = "أدخل رمز التحقق المكوّن من 6 أرقام."
                                    } else {
                                        finishSignIn(PhoneAuthProvider.getCredential(id, code))
                                    }
                                } else {
                                    val normalized = phone.trim().replace(" ", "")
                                    if (activity == null) {
                                        message = "تعذر بدء التحقق على هذا الجهاز."
                                    } else if (!normalized.startsWith("+") || normalized.length < 9) {
                                        message = "اكتب الرقم كاملاً مع مفتاح الدولة، مثل +905xxxxxxxxx"
                                    } else {
                                        busy = true
                                        val options = PhoneAuthOptions.newBuilder(auth)
                                            .setPhoneNumber(normalized)
                                            .setTimeout(60L, TimeUnit.SECONDS)
                                            .setActivity(activity)
                                            .setCallbacks(callbacks)
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = AuthOrange, contentColor = Color.Black)
                        ) {
                            if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            else Text(if (waitingForCode) "تأكيد الرمز والدخول" else "إرسال رمز التحقق", fontWeight = FontWeight.Bold)
                        }
                        if (waitingForCode) {
                            TextButton(onClick = {
                                waitingForCode = false
                                verificationId = null
                                code = ""
                                message = ""
                            }) { Text("تغيير رقم الهاتف", color = AuthOrange) }
                        }
                        if (message.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(message, color = if (message.contains("نجاح")) AuthOrange else Color(0xFFFFB4AB), textAlign = TextAlign.Center, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("باستخدامك وصال، يمكنك التواصل مع مجتمعك", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
