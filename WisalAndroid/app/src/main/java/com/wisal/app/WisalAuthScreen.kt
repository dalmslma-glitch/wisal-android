package com.wisal.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

private val FreeAuthOrange = Color(0xFFE7A84B)
private val FreeAuthBackground = Color(0xFF090D18)
private val FreeAuthPanel = Color(0xFF151D2D)

@Composable
fun WisalAuthScreen(onSignedIn: () -> Unit) {
    var phoneScreen by remember { mutableStateOf(false) }
    if (phoneScreen) {
        PhoneAuthScreen(onSignedIn = onSignedIn)
    } else {
        EmailAuthScreen(onSignedIn = onSignedIn, onPhoneSelected = { phoneScreen = true })
    }
}

@Composable
private fun EmailAuthScreen(onSignedIn: () -> Unit, onPhoneSelected: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme(primary = FreeAuthOrange, background = FreeAuthBackground, surface = FreeAuthPanel)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Box(Modifier.fillMaxSize().background(FreeAuthBackground)) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.height(28.dp))
                    Text("W", color = FreeAuthOrange, fontSize = 46.sp, fontWeight = FontWeight.Black)
                    Text("وِصال", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("رسائلك تقرّب المسافات", color = Color(0xFFB9C4D8), fontSize = 15.sp)
                    Spacer(Modifier.height(26.dp))
                    Surface(color = FreeAuthPanel, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = FreeAuthOrange, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(if (createAccount) "أنشئ حساب وصال مجاناً" else "تسجيل الدخول", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text("التسجيل بالبريد الإلكتروني لا يحتاج إلى تفعيل الفوترة", color = Color(0xFFB9C4D8), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(18.dp))
                            if (createAccount) {
                                OutlinedTextField(
                                    value = displayName, onValueChange = { displayName = it },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    label = { Text("الاسم") },
                                    colors = authFieldColors()
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                            OutlinedTextField(
                                value = email, onValueChange = { email = it.trim() },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                label = { Text("البريد الإلكتروني") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = FreeAuthOrange) },
                                colors = authFieldColors()
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = password, onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                label = { Text("كلمة المرور (6 أحرف على الأقل)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = FreeAuthOrange) },
                                trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "إخفاء" else "إظهار", color = FreeAuthOrange) } },
                                colors = authFieldColors()
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    message = ""
                                    if (!email.contains("@")) message = "أدخل بريداً إلكترونياً صحيحاً."
                                    else if (password.length < 6) message = "كلمة المرور يجب ألا تقل عن 6 أحرف."
                                    else if (createAccount && displayName.isBlank()) message = "اكتب اسمك للمتابعة."
                                    else {
                                        busy = true
                                        val task = if (createAccount) auth.createUserWithEmailAndPassword(email, password)
                                                   else auth.signInWithEmailAndPassword(email, password)
                                        task.addOnCompleteListener { result ->
                                            busy = false
                                            if (result.isSuccessful) {
                                                val user = auth.currentUser
                                                if (createAccount && user != null) {
                                                    val profile = com.google.firebase.auth.userProfileChangeRequest {
                                                        this.displayName = displayName.trim()
                                                    }
                                                    user.updateProfile(profile).addOnCompleteListener { onSignedIn() }
                                                } else onSignedIn()
                                            } else {
                                                message = result.exception?.localizedMessage ?: "تعذر إكمال التسجيل. تحقق من البيانات."
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                enabled = !busy,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FreeAuthOrange, contentColor = Color(0xFF101522))
                            ) {
                                if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                                else Text(if (createAccount) "إنشاء حساب مجاني" else "دخول", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { createAccount = !createAccount; message = "" }) {
                                Text(if (createAccount) "لديك حساب؟ سجّل الدخول" else "ليس لديك حساب؟ أنشئ حساباً", color = FreeAuthOrange)
                            }
                            if (message.isNotBlank()) {
                                Text(message, color = Color(0xFFFFB4AB), textAlign = TextAlign.Center, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onPhoneSelected,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Phone, null, tint = FreeAuthOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("تسجيل الدخول برقم الهاتف")
                    }
                    Text("خيار الهاتف محفوظ للتجربة، وقد يتطلب إرسال SMS حقيقياً خطة مدفوعة.", color = Color(0xFF7F8CA3), fontSize = 11.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = FreeAuthOrange,
    unfocusedBorderColor = Color(0xFF526078),
    focusedLabelColor = FreeAuthOrange,
    cursorColor = FreeAuthOrange
)
