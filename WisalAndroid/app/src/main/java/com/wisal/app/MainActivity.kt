package com.wisal.app

import android.os.Bundle
import android.content.Intent
import android.provider.OpenableColumns
import android.widget.Toast
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

private val Navy = Color(0xFF090909)
private val Navy2 = Color(0xFFFF8A00)
private val Gold = Color(0xFFFF8A00)
private val Cream = Color(0xFF121212)
private val Ink = Color(0xFFF2F2F2)
private val Muted = Color(0xFF929292)
private val Panel = Color(0xFF242424)

data class Person(val name: String, val message: String, val time: String, val initials: String, val unread: Int = 0, val uid: String? = null)
private val people = listOf(
    Person("أحمد محمد", "السلام عليكم، كيف حالك؟", "٩:٤٢ م", "أم", 2),
    Person("سارة", "أرسلت صورة", "٨:١٥ م", "س"),
    Person("مجموعة الأصدقاء", "علي: نلتقي غداً إن شاء الله", "٧:٥٠ م", "أص", 4),
    Person("خالد", "شكراً لك 🌟", "٦:٣٠ م", "خ")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 901)
        }
        setContent {
            var signedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
            LaunchedEffect(signedIn) {
                if (signedIn) {
                    WisalMessaging.saveMyProfile(onResult = { success, error ->
                        val message = if (success) {
                            "تم حفظ حسابك في وصال بنجاح."
                        } else {
                            "تعذر حفظ حسابك: ${error ?: "خطأ غير معروف"}"
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    })
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(uid)
                                .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                        }
                    }
                }
            }
            if (signedIn) WisalApp(onSignOut = { FirebaseAuth.getInstance().signOut(); signedIn = false }) else WisalAuthScreen(onSignedIn = { signedIn = true })
        }
    }
}

@Composable
fun WisalApp(onSignOut: () -> Unit) {
    var tab by remember { mutableStateOf("الدردشات") }
    var selected by remember { mutableStateOf<Person?>(null) }
    var selectedGroup by remember { mutableStateOf<WisalGroup?>(null) }
    var showSplash by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1600); showSplash = false }
    MaterialTheme(colorScheme = darkColorScheme(primary = Navy2, secondary = Gold, background = Cream, surface = Panel)) {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            if (showSplash) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    WisalAnimatedSplash()
                }
            } else if (selected != null) {
                ChatScreen(person = selected!!, onBack = { selected = null })
            } else if (selectedGroup != null) {
                GroupChatScreen(group = selectedGroup!!, onBack = { selectedGroup = null })
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
                        Header(tab, onSearchClick = { tab = "الدردشات" }, onMenuClick = { showSettings = true })
                        when (tab) {
                            "الدردشات" -> Chats { selected = it }
                            "الحالات" -> StatusPage()
                            "المجموعات" -> GroupsPage(onOpen = { selectedGroup = it })
                            "جهات الاتصال" -> ContactsPage { selected = it }
                            else -> ProfilePage()
                        }
                    }
                }
                if (showSettings) WisalSettingsDialog(onDismiss = { showSettings = false }, onSignOut = onSignOut)
            }
        }
    }
}

@Composable
fun Header(title: String, onSearchClick: () -> Unit, onMenuClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Navy).padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("WISAL", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, null, tint = Color.White) }
        IconButton(onClick = onMenuClick) { Icon(Icons.Default.MoreVert, null, tint = Gold) }
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
    var cloudMessages by remember(person.uid) { mutableStateOf(emptyList<WisalMessage>()) }
    var sendError by remember { mutableStateOf("") }
    var uploadingImage by remember { mutableStateOf(false) }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val emojiPickerVisible = remember { mutableStateOf(false) }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && person.uid != null) {
            uploadingImage = true
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "مرفق"
            WisalMessaging.sendAttachment(person.uid, uri, mime, fileName,
                onError = { sendError = it },
                onComplete = { uploadingImage = false }
            )
        }
    }

    DisposableEffect(person.uid) {
        val otherUid = person.uid
        if (otherUid != null) {
            val stop = WisalMessaging.observeMessages(
                otherUid,
                onChange = { cloudMessages = it },
                onError = { sendError = it }
            )
            WisalMessaging.markRead(otherUid)
            onDispose { stop() }
        } else onDispose { }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF101827))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF111B2B)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) }
            AvatarFor(person.name, 40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, color = Gold, fontWeight = FontWeight.Bold)
                Text(if (person.uid == null) "معاينة" else "محادثة وصال", color = Color(0xFF9EABC0), fontSize = 11.sp)
            }
            IconButton(onClick = { sendError = "المكالمات الصوتية ستضاف بعد إعداد خدمة الاتصال." }) { Icon(Icons.Default.Call, null, tint = Color.White) }
            IconButton(onClick = { sendError = "مكالمات الفيديو ستضاف بعد إعداد خدمة الاتصال." }) { Icon(Icons.Default.Videocam, null, tint = Color.White) }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            reverseLayout = false
        ) {
            if (person.uid == null) {
                item { Bubble("هذه محادثة تجريبية قديمة. ابحث عن حساب وصال برقم هاتفه لبدء محادثة حقيقية.", false) }
                items(sent) { Bubble(it, true) }
            } else {
                items(cloudMessages, key = { it.id }) { item ->
                    val mine = item.senderId == myUid
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
                        Surface(
                            color = if (mine) Color(0xFF234B43) else Color(0xFF202C3D),
                            shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp
                        ) {
                            Column(Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
                                if (item.type == "image" && item.mediaUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.mediaUrl,
                                        contentDescription = "صورة مرسلة",
                                        modifier = Modifier.widthIn(max = 280.dp).heightIn(max = 360.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit,
                                        crossfade = true
                                    )
                                } else if (item.mediaUrl.isNotBlank()) {
                                    TextButton(onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse(item.mediaUrl)))
                                        }.onFailure { sendError = "لا يوجد تطبيق مناسب لفتح هذا المرفق." }
                                    }) {
                                        Icon(Icons.Default.AttachFile, null, tint = Gold)
                                        Text(item.fileName.ifBlank { item.text }, color = Color.White)
                                    }
                                } else {
                                    Text(item.text, color = Color.White, fontSize = 14.sp)
                                }
                                if (mine) {
                                    val delivered = item.deliveredTo.contains(person.uid)
                                    val read = item.readBy.contains(person.uid)
                                    Text(
                                        when { read -> "✓✓"; delivered -> "✓✓"; else -> "✓" },
                                        color = if (read) Color(0xFF61C8FF) else Color(0xFFB8C6D8),
                                        fontSize = 10.sp, textAlign = TextAlign.End,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
        if (sendError.isNotBlank()) {
            Text(sendError, color = Color(0xFFFFB4AB), modifier = Modifier.padding(horizontal = 12.dp), fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF111B2B)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { emojiPickerVisible.value = !emojiPickerVisible.value }) { Icon(Icons.Default.EmojiEmotions, null, tint = Gold) }
            IconButton(onClick = {
                if (person.uid == null) sendError = "ابدأ محادثة حقيقية أولاً."
                else showAttachmentOptions = true
            }, enabled = !uploadingImage) {
                if (uploadingImage) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Gold)
                else Icon(Icons.Default.AttachFile, null, tint = Gold)
            }
            OutlinedTextField(
                value = message, onValueChange = { message = it; sendError = "" },
                modifier = Modifier.weight(1f), placeholder = { Text("اكتب رسالة...", color = Muted) },
                shape = RoundedCornerShape(24.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Gold, unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Panel, unfocusedContainerColor = Panel, cursorColor = Gold
                )
            )
            IconButton(onClick = {
                if (message.isNotBlank()) {
                    val outgoing = message.trim()
                    if (person.uid != null) {
                        WisalMessaging.sendText(person.uid, outgoing) { sendError = it }
                    } else sent = sent + outgoing
                    message = ""
                }
            }) { Icon(Icons.Default.Send, null, tint = Gold) }
        }
        if (showAttachmentOptions) {
            AlertDialog(
                onDismissRequest = { showAttachmentOptions = false },
                title = { Text("إرفاق محتوى", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("صورة" to "image/*", "فيديو" to "video/*", "صوت" to "audio/*", "ملف أو مستند" to "*/*").forEach { (label, mime) ->
                            TextButton(onClick = {
                                showAttachmentOptions = false
                                attachmentPicker.launch(mime)
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(label, color = Gold)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAttachmentOptions = false }) { Text("إغلاق", color = Gold) } },
                containerColor = Panel
            )
        }
        if (emojiPickerVisible.value) {
            Row(Modifier.fillMaxWidth().background(Panel).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("😀","😂","🥹","😭","❤️","👍","🙏","😍","😊","😎","🥰","😘","😅","🤣","😉","🤔","😴","😢","😡","👏","🎉","🔥","💯","🤝").chunked(8).forEach { group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            group.forEach { emoji -> Text(emoji, modifier = Modifier.clickable { message += emoji }, fontSize = 23.sp) }
                        }
                    }
                }
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
fun GroupsPage(onOpen: (WisalGroup) -> Unit) {
    var groups by remember { mutableStateOf(emptyList<WisalGroup>()) }
    var error by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val stop = WisalMessaging.observeMyGroups({ groups = it }, { error = it })
        onDispose { stop() }
    }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("مجموعات وصال", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("مجموعاتك ومحادثاتها", color = Muted, fontSize = 12.sp)
            }
            Button(onClick = { showCreate = true }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
                Text("إنشاء", color = Color.Black)
            }
        }
        if (error.isNotBlank()) Text(error, color = Color(0xFFFFB4AB), fontSize = 12.sp)
        if (groups.isEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text("لا توجد مجموعات بعد. أنشئ مجموعة جديدة للبدء.", color = Muted)
        } else {
            LazyColumn {
                items(groups, key = { it.id }) { group ->
                    Row(Modifier.fillMaxWidth().clickable { onOpen(group) }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar("مج", 48)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(group.lastMessage.ifBlank { "مجموعة جديدة" }, color = Muted, fontSize = 12.sp, maxLines = 1)
                        }
                        Text("${group.members.size} عضو", color = Muted, fontSize = 11.sp)
                    }
                    HorizontalDivider(color = Color(0xFF292929))
                }
            }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { if (!creating) showCreate = false },
            title = { Text("إنشاء مجموعة", color = Color.White) },
            text = { OutlinedTextField(value = groupName, onValueChange = { groupName = it.take(60) }, label = { Text("اسم المجموعة") }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = !creating, onClick = {
                    creating = true
                    WisalMessaging.createGroup(groupName) { _, problem ->
                        creating = false
                        if (problem == null) { groupName = ""; showCreate = false } else error = problem
                    }
                }) { Text(if (creating) "جارٍ الإنشاء..." else "إنشاء", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("إلغاء", color = Muted) } },
            containerColor = Panel
        )
    }
}

@Composable
fun GroupChatScreen(group: WisalGroup, onBack: () -> Unit) {
    var messages by remember(group.id) { mutableStateOf(emptyList<WisalMessage>()) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    DisposableEffect(group.id) {
        val stop = WisalMessaging.observeGroupMessages(group.id, { messages = it }, { error = it })
        onDispose { stop() }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF101827))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF111B2B)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) }
            Avatar("مج", 40)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(group.name, color = Gold, fontWeight = FontWeight.Bold)
                Text("${group.members.size} عضو", color = Muted, fontSize = 11.sp)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
            items(messages, key = { it.id }) { msg ->
                val mine = msg.senderId == uid
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.Start else Alignment.End) {
                    Text(if (mine) "أنت" else "عضو", color = Gold, fontSize = 10.sp)
                    Surface(color = if (mine) Color(0xFF234B43) else Panel, shape = RoundedCornerShape(14.dp)) {
                        Text(msg.text, Modifier.padding(12.dp), color = Color.White)
                    }
                }
                Spacer(Modifier.height(7.dp))
            }
        }
        if (error.isNotBlank()) Text(error, color = Color(0xFFFFB4AB), fontSize = 11.sp)
        Row(Modifier.fillMaxWidth().background(Color(0xFF111B2B)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f), placeholder = { Text("اكتب رسالة للمجموعة...") }, singleLine = true)
            IconButton(onClick = {
                if (draft.isNotBlank()) {
                    val outgoing = draft
                    WisalMessaging.sendGroupText(group.id, outgoing) { error = it }
                    draft = ""
                }
            }) { Icon(Icons.Default.Send, null, tint = Gold) }
        }
    }
}

@Composable
fun ContactsPage(onOpen: (Person) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("ابدأ محادثة جديدة", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("وصال لا يقترح أشخاصاً مجهولين. أضف جهة اتصال تعرفها باستخدام رابط دعوة أو رمز مشاركة عندما تتوفر هذه الميزة.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Default.People, contentDescription = null, tint = Gold, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(8.dp))
                Text("البحث عن الحسابات قيد التجهيز", color = Color.White, fontWeight = FontWeight.Bold)
                Text("البحث الآمن عن مستخدمين يحتاج إلى خدمة خادم. أوقفناه مؤقتاً حتى يبقى التطبيق متوافقاً مع خطة Firebase المجانية ولا نكشف بيانات المستخدمين.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WisalSettingsDialog(onDismiss: () -> Unit, onSignOut: () -> Unit) {
    val account = FirebaseAuth.getInstance().currentUser
    var confirmSignOut by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعدادات وصال", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الحساب", color = Gold, fontWeight = FontWeight.Bold)
                Text(account?.email ?: account?.phoneNumber ?: "حساب وصال", color = Color.White)
                Text("معرّف الحساب: ${account?.uid?.take(12) ?: "غير متاح"}…", color = Muted, fontSize = 11.sp)
                HorizontalDivider(color = Color(0xFF444444))
                Text("الخصوصية: لا تشارك رمز الدخول أو كلمة المرور مع أي شخص.", color = Color.White, fontSize = 13.sp)
                Text("اللغات الإضافية وتشفير طرف إلى طرف والمكالمات تحتاج إلى تطوير واختبار مستقل قبل إعلان جاهزيتها.", color = Muted, fontSize = 12.sp)
                if (confirmSignOut) Text("هل تريد تسجيل الخروج من هذا الحساب؟", color = Color(0xFFFFB4AB))
            }
        },
        confirmButton = {
            if (confirmSignOut) TextButton(onClick = onSignOut) { Text("تأكيد الخروج", color = Color(0xFFFF8A80)) }
            else TextButton(onClick = { confirmSignOut = true }) { Text("تسجيل الخروج", color = Color(0xFFFF8A80)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = Gold) } },
        containerColor = Color(0xFF242424),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
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
