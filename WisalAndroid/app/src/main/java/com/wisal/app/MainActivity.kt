package com.wisal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuth

private val Navy = Color(0xFF090909)
private val Navy2 = Color(0xFFFF8A00)
private val Gold = Color(0xFFFF8A00)
private val Cream = Color(0xFF121212)
private val Ink = Color(0xFFF2F2F2)
private val Muted = Color(0xFF929292)
private val Panel = Color(0xFF242424)

data class Person(val name: String, val message: String, val time: String, val initials: String, val unread: Int = 0)
private val people = listOf(
    Person("أحمد محمد", "السلام عليكم، كيف حالك؟", "٩:٤٢ م", "أم", 2),
    Person("سارة", "أرسلت صورة", "٨:١٥ م", "س"),
    Person("مجموعة الأصدقاء", "علي: نلتقي غداً إن شاء الله", "٧:٥٠ م", "أص", 4),
    Person("خالد", "شكراً لك 🌟", "٦:٣٠ م", "خ")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var signedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
            if (signedIn) WisalApp() else PhoneAuthScreen(onSignedIn = { signedIn = true })
        }
    }
}

@Composable
fun WisalApp() {
    var tab by remember { mutableStateOf("الدردشات") }
    var selected by remember { mutableStateOf<Person?>(null) }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1600); showSplash = false }
    MaterialTheme(colorScheme = darkColorScheme(primary = Navy2, secondary = Gold, background = Cream, surface = Panel)) {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            if (showSplash) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    WisalPosterImage("SPLASH_ART", Modifier.fillMaxSize(), ContentScale.Crop)
                }
            } else if (selected != null) {
                ChatScreen(person = selected!!, onBack = { selected = null })
            } else {
                Scaffold(
                    containerColor = Cream,
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF101010), contentColor = Gold) {
                            val tabs = listOf(
                                "الدردشات" to Icons.Default.ChatBubble,
                                "الحالات" to Icons.Default.Circle,
                                "المجموعات" to Icons.Default.Groups,
                                "جهات الاتصال" to Icons.Default.Contacts,
                                "حسابي" to Icons.Default.Person
                            )
                            tabs.forEach { (label, icon) ->
                                NavigationBarItem(
                                    selected = tab == label,
                                    onClick = { tab = label },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Gold, selectedTextColor = Gold,
                                        unselectedIconColor = Color(0xFF777777), unselectedTextColor = Color(0xFF777777),
                                        indicatorColor = Color(0xFF302015)
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        Header(tab)
                        when (tab) {
                            "الدردشات" -> Chats { selected = it }
                            "الحالات" -> StatusPage()
                            "المجموعات" -> GroupsPage()
                            "جهات الاتصال" -> ContactsPage { selected = it }
                            else -> ProfilePage()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Header(title: String) {
    Row(
        Modifier.fillMaxWidth().background(Navy).padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("WISAL", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = {}) { Icon(Icons.Default.Search, null, tint = Color.White) }
        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Gold) }
    }
}

@Composable
fun Chats(onOpen: (Person) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            placeholder = { Text("بحث عن الأشخاص أو الرسائل", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Gold) },
            shape = RoundedCornerShape(22.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Gold, unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Panel, unfocusedContainerColor = Panel,
                cursorColor = Gold
            )
        )
        StatusStrip()
        LazyColumn {
            items(people.filter { it.name.contains(query) || it.message.contains(query) }) { person ->
                ChatRow(person, onClick = { onOpen(person) })
                HorizontalDivider(color = Color(0xFF292929), thickness = 0.7.dp)
            }
        }
    }
}

@Composable
fun StatusStrip() {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)) {
        Text("الحالات", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf("أنت", "Menna", "Shaker", "Sohaila", "محمد").forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(54.dp)) {
                    Box(Modifier.size(50.dp).background(Gold, CircleShape).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (index == 0) Text("+", color = Gold, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        else WisalPosterImage(listOf("AVATAR_MARYAM", "AVATAR_SAIF", "AVATAR_AHMED", "AVATAR_LINA")[index - 1], Modifier.fillMaxSize().clip(CircleShape), ContentScale.Crop)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, color = Color.LightGray, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun ChatRow(person: Person, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarFor(person.name, 52)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(person.message, color = Muted, fontSize = 12.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(person.time, color = Muted, fontSize = 11.sp)
            if (person.unread > 0) {
                Spacer(Modifier.height(5.dp))
                Box(Modifier.background(Gold, CircleShape).size(22.dp), contentAlignment = Alignment.Center) {
                    Text(person.unread.toString(), color = Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AvatarFor(name: String, size: Int = 48) {
    val asset = when (name) {
        "أحمد محمد" -> "AVATAR_AHMED"
        "سارة" -> "AVATAR_LINA"
        "خالد" -> "AVATAR_SAIF"
        "مجموعة الأصدقاء" -> "GROUP_ICON"
        else -> "AVATAR_PROFILE"
    }
    Box(Modifier.size(size.dp).background(Gold, CircleShape).padding(2.dp)) {
        WisalPosterImage(asset, Modifier.fillMaxSize().clip(CircleShape), ContentScale.Crop)
    }
}

@Composable
fun Avatar(text: String, size: Int = 48) {
    Box(Modifier.size(size.dp).background(Gold, CircleShape).padding(2.dp), contentAlignment = Alignment.Center) {
        WisalPosterImage(if (text == "مج") "GROUP_ICON" else "AVATAR_PROFILE", Modifier.fillMaxSize().clip(CircleShape), ContentScale.Crop)
    }
}

@Composable
fun ChatScreen(person: Person, onBack: () -> Unit) {
    var message by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(listOf<String>()) }
    Column(Modifier.fillMaxSize().background(Color(0xFF101010))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF090909)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) }
            AvatarFor(person.name, 40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, color = Gold, fontWeight = FontWeight.Bold)
                Text("متصل الآن", color = Gold, fontSize = 11.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Call, null, tint = Color.White) }
            IconButton(onClick = {}) { Icon(Icons.Default.Videocam, null, tint = Color.White) }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.Bottom) {
            Bubble("مرحباً بك في وصال 💙", false)
            Bubble("هذه معاينة لتصميم المحادثة.", false)
            sent.forEach { Bubble(it, true) }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF191919)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) { Icon(Icons.Default.AttachFile, null, tint = Navy2) }
            OutlinedTextField(
                value = message, onValueChange = { message = it },
                modifier = Modifier.weight(1f), placeholder = { Text("اكتب نصاً...", color = Muted) },
                shape = RoundedCornerShape(24.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Gold, unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Panel, unfocusedContainerColor = Panel,
                    cursorColor = Gold
                )
            )
            IconButton(onClick = { if (message.isNotBlank()) { sent = sent + message; message = "" } }) {
                Icon(Icons.Default.Send, null, tint = Gold)
            }
        }
    }
}

@Composable
fun Bubble(text: String, mine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
        Surface(color = if (mine) Gold else Panel, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
            Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = if (mine) Color.Black else Color.White, fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun StatusPage() {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("الحالات", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("شارك لحظاتك مع جهات اتصالك.", color = Muted)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar("وص", 54)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("حالتي", fontWeight = FontWeight.Bold, color = Ink)
                Text("أضف تحديثاً جديداً", color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("آخر التحديثات", color = Gold, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        people.take(3).forEach { person ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(Gold, CircleShape).padding(3.dp)) { AvatarFor(person.name, 48) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(person.name, color = Ink, fontWeight = FontWeight.Bold)
                    Text("تحديث حالة", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun GroupsPage() {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("مجموعات وصال", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        listOf("العائلة", "الأصدقاء", "مجتمع وصال").forEach {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar("مج", 48); Spacer(Modifier.width(12.dp)); Text(it, color = Ink, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ContactsPage(onOpen: (Person) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(people) { ChatRow(it.copy(message = "متاح للتواصل"), onClick = { onOpen(it) }) }
    }
}

@Composable
fun ProfilePage() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp)); Avatar("وص", 112)
        Spacer(Modifier.height(14.dp))
        Text("ملفي الشخصي", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("مرحباً بك في مجتمع وصال", color = Muted)
        Spacer(Modifier.height(22.dp))
        listOf("تعديل الاسم والصورة", "الخصوصية والأمان", "الإشعارات", "حول وصال").forEach {
            Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp), color = Panel, shape = RoundedCornerShape(14.dp)) {
                Text(it, Modifier.padding(16.dp), color = Color.White)
            }
        }
    }
}
