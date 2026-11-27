package com.wisal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private val Navy = Color(0xFF081B35)
private val Navy2 = Color(0xFF102D50)
private val Gold = Color(0xFFE3BD68)
private val Cream = Color(0xFFF5F1E8)
private val Ink = Color(0xFF14243A)
private val Muted = Color(0xFF8190A3)

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
        setContent { WisalApp() }
    }
}

@Composable
fun WisalApp() {
    var tab by remember { mutableStateOf("الدردشات") }
    var selected by remember { mutableStateOf<Person?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Navy2, secondary = Gold, background = Cream)) {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            if (selected != null) {
                ChatScreen(person = selected!!, onBack = { selected = null })
            } else {
                Scaffold(
                    containerColor = Cream,
                    bottomBar = {
                        NavigationBar(containerColor = Navy, contentColor = Gold) {
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
                                        unselectedIconColor = Color.LightGray, unselectedTextColor = Color.LightGray,
                                        indicatorColor = Navy2
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
            Text("وصال", color = Gold, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White.copy(alpha = .82f), fontSize = 13.sp)
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
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            placeholder = { Text("ابحث في المحادثات") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(18.dp), singleLine = true
        )
        LazyColumn {
            items(people.filter { it.name.contains(query) || it.message.contains(query) }) { person ->
                ChatRow(person, onClick = { onOpen(person) })
                HorizontalDivider(color = Color(0xFFE6E0D5), thickness = 0.7.dp)
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
        Avatar(person.initials, 52)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(person.message, color = Muted, fontSize = 13.sp, maxLines = 1)
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
fun Avatar(text: String, size: Int = 48) {
    Box(
        Modifier.size(size.dp).background(Navy2, CircleShape),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
fun ChatScreen(person: Person, onBack: () -> Unit) {
    var message by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(listOf<String>()) }
    Column(Modifier.fillMaxSize().background(Color(0xFFF0EBDD))) {
        Row(Modifier.fillMaxWidth().background(Navy).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) }
            Avatar(person.initials, 40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, color = Gold, fontWeight = FontWeight.Bold)
                Text("محادثة تجريبية", color = Color.LightGray, fontSize = 11.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Call, null, tint = Color.White) }
            IconButton(onClick = {}) { Icon(Icons.Default.Videocam, null, tint = Color.White) }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.Bottom) {
            Bubble("مرحباً بك في وصال 💙", false)
            Bubble("هذه معاينة لتصميم المحادثة.", false)
            sent.forEach { Bubble(it, true) }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) { Icon(Icons.Default.AttachFile, null, tint = Navy2) }
            OutlinedTextField(
                value = message, onValueChange = { message = it },
                modifier = Modifier.weight(1f), placeholder = { Text("اكتب رسالة...") },
                shape = RoundedCornerShape(24.dp), singleLine = true
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
        Surface(color = if (mine) Color(0xFFE7D5A9) else Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
            Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Ink, fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun StatusPage() {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("الحالات", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("تابع تحديثات أصدقائك وشارك لحظاتك.", color = Muted)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar("+", 54); Spacer(Modifier.width(12.dp))
            Column { Text("حالتي", fontWeight = FontWeight.Bold, color = Ink); Text("أضف تحديثاً جديداً", color = Muted, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(20.dp))
        people.take(3).forEach { Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(Gold, CircleShape).padding(3.dp)) { Avatar(it.initials, 48) }
            Spacer(Modifier.width(12.dp)); Column { Text(it.name, color = Ink, fontWeight = FontWeight.Bold); Text("منذ قليل", color = Muted, fontSize = 12.sp) }
        } }
    }
}

@Composable
fun GroupsPage() {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("مجموعات وصال", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
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
        Spacer(Modifier.height(24.dp)); Avatar("وص", 92)
        Spacer(Modifier.height(14.dp))
        Text("ملفي الشخصي", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("مرحباً بك في مجتمع وصال", color = Muted)
        Spacer(Modifier.height(22.dp))
        listOf("تعديل الاسم والصورة", "الخصوصية والأمان", "الإشعارات", "حول وصال").forEach {
            Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp), color = Color.White, shape = RoundedCornerShape(14.dp)) {
                Text(it, Modifier.padding(16.dp), color = Ink)
            }
        }
    }
}
