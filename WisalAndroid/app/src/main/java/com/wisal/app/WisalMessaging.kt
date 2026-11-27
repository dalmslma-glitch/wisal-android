package com.wisal.app

import android.net.Uri
import java.util.UUID
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.security.MessageDigest

data class WisalUser(
    val uid: String = "",
    val phone: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val lastSeen: Long = 0L
)

data class WisalMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val fileName: String = "",
    val createdAt: Long = 0L,
    val deliveredTo: List<String> = emptyList(),
    val readBy: List<String> = emptyList(),
    val type: String = "text"
)

/**
 * Cloud Firestore data layer. Chat IDs are deterministic for a pair of accounts;
 * message status is stored per recipient so sent/delivered/read are distinct states.
 */
object WisalMessaging {
    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    fun saveMyProfile(
        displayName: String = "",
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult?.invoke(false, "لم يتم العثور على حساب مسجل الدخول.")
            return
        }
        val data = hashMapOf(
            "uid" to user.uid,
            "phone" to (user.phoneNumber ?: ""),
            "email" to (user.email ?: ""),
            "displayName" to (displayName.ifBlank { user.displayName ?: "" }),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(user.uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.i("WisalMessaging", "Profile saved for uid=${user.uid}")
                onResult?.invoke(true, null)
            }
            .addOnFailureListener { error ->
                val message = error.localizedMessage ?: "تعذر حفظ بيانات الحساب في Firestore."
                android.util.Log.e("WisalMessaging", "Profile save failed", error)
                onResult?.invoke(false, message)
            }
    }

    fun chatId(otherUid: String): String {
        val ids = listOf(auth.currentUser?.uid ?: "unknown", otherUid).sorted()
        return MessageDigest.getInstance("SHA-256")
            .digest(ids.joinToString("_").toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun findUserByPhone(phone: String, onResult: (WisalUser?, String?) -> Unit) {
        val normalized = phone.trim().replace(" ", "")
        if (normalized.isBlank()) { onResult(null, "أدخل رقم الهاتف مع مفتاح الدولة."); return }
        db.collection("users").whereEqualTo("phone", normalized).limit(1).get()
            .addOnSuccessListener { result ->
                val item = result.documents.firstOrNull()
                if (item == null || item.id == auth.currentUser?.uid) onResult(null, "لم نعثر على حساب وصال بهذا الرقم.")
                else onResult(item.toObject(WisalUser::class.java)?.copy(uid = item.id), null)
            }
            .addOnFailureListener { onResult(null, it.localizedMessage ?: "تعذر البحث عن الحساب.") }
    }

    fun observeMessages(otherUid: String, onChange: (List<WisalMessage>) -> Unit, onError: (String) -> Unit): () -> Unit {
        val myUid = auth.currentUser?.uid ?: return {}
        val ref = db.collection("chats").document(chatId(otherUid)).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(200)
        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error.localizedMessage ?: "تعذر تحميل الرسائل."); return@addSnapshotListener }
            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(WisalMessage::class.java)?.copy(id = doc.id)
            }.orEmpty()
            onChange(messages)
            snapshot?.documents?.filter { doc ->
                val sender = doc.getString("senderId")
                sender != myUid && !(doc.get("readBy") as? List<*>)?.contains(myUid).orFalse()
            }?.forEach { doc ->
                doc.reference.update(
                    mapOf(
                        "deliveredTo" to FieldValue.arrayUnion(myUid),
                        "readBy" to FieldValue.arrayUnion(myUid)
                    )
                )
            }
        }
        return { registration.remove() }
    }

    fun sendText(otherUid: String, text: String, onError: (String) -> Unit = {}) {
        val me = auth.currentUser?.uid ?: run { onError("سجّل الدخول أولاً."); return }
        val clean = text.trim()
        if (clean.isEmpty()) return
        val chat = db.collection("chats").document(chatId(otherUid))
        val message = hashMapOf(
            "senderId" to me,
            "text" to clean,
            "type" to "text",
            "createdAt" to System.currentTimeMillis(),
            "deliveredTo" to emptyList<String>(),
            "readBy" to listOf(me)
        )
        chat.set(mapOf(
            "members" to listOf(me, otherUid),
            "lastMessage" to clean,
            "lastSenderId" to me,
            "updatedAt" to FieldValue.serverTimestamp()
        ), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                chat.collection("messages").add(message).addOnFailureListener {
                    onError(it.localizedMessage ?: "تعذر إرسال الرسالة.")
                }
            }.addOnFailureListener { onError(it.localizedMessage ?: "تعذر إنشاء المحادثة.") }
    }

    fun sendImage(otherUid: String, uri: Uri, onError: (String) -> Unit = {}, onComplete: () -> Unit = {}) {
        val me = auth.currentUser?.uid ?: run { onError("سجّل الدخول أولاً."); onComplete(); return }
        val chatId = chatId(otherUid)
        val chat = db.collection("chats").document(chatId)
        chat.set(
            mapOf(
                "members" to listOf(me, otherUid),
                "lastMessage" to "📷 صورة",
                "lastSenderId" to me,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).addOnSuccessListener {
            val fileRef = FirebaseStorage.getInstance().reference
                .child("chat-media/$chatId/${UUID.randomUUID()}.jpg")
            fileRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw (task.exception ?: Exception("فشل رفع الصورة."))
                    fileRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    chat.collection("messages").add(hashMapOf(
                        "senderId" to me,
                        "text" to "صورة",
                        "mediaUrl" to downloadUri.toString(),
                        "type" to "image",
                        "createdAt" to System.currentTimeMillis(),
                        "deliveredTo" to emptyList<String>(),
                        "readBy" to listOf(me)
                    )).addOnFailureListener {
                        onError(it.localizedMessage ?: "تعذر إرسال الصورة.")
                    }.addOnCompleteListener { onComplete() }
                }
                .addOnFailureListener {
                    onError(it.localizedMessage ?: "تعذر رفع الصورة.")
                    onComplete()
                }
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "تعذر إنشاء المحادثة.")
            onComplete()
        }
    }


    fun sendAttachment(
        otherUid: String,
        uri: Uri,
        mimeType: String,
        fileName: String,
        onError: (String) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        val me = auth.currentUser?.uid ?: run { onError("سجّل الدخول أولاً."); onComplete(); return }
        val conversationId = chatId(otherUid)
        val chat = db.collection("chats").document(conversationId)
        val safeName = fileName.ifBlank { "attachment" }.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100)
        val extension = safeName.substringAfterLast('.', "bin").take(10)
        val kind = when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "file"
        }
        val preview = when (kind) {
            "image" -> "📷 صورة"
            "video" -> "🎬 فيديو"
            "audio" -> "🎵 مقطع صوتي"
            else -> "📎 $fileName"
        }
        chat.set(
            mapOf("members" to listOf(me, otherUid), "lastMessage" to preview,
                "lastSenderId" to me, "updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).addOnSuccessListener {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("chat-media/$conversationId/${UUID.randomUUID()}.$extension")
            storageRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw (task.exception ?: Exception("فشل رفع المرفق."))
                    storageRef.downloadUrl
                }
                .addOnSuccessListener { download ->
                    chat.collection("messages").add(hashMapOf(
                        "senderId" to me, "text" to preview, "mediaUrl" to download.toString(),
                        "fileName" to fileName, "type" to kind,
                        "createdAt" to System.currentTimeMillis(),
                        "deliveredTo" to emptyList<String>(), "readBy" to listOf(me)
                    )).addOnFailureListener { onError(it.localizedMessage ?: "تعذر إرسال المرفق.") }
                        .addOnCompleteListener { onComplete() }
                }
                .addOnFailureListener { onError(it.localizedMessage ?: "تعذر رفع المرفق."); onComplete() }
        }.addOnFailureListener { onError(it.localizedMessage ?: "تعذر إنشاء المحادثة."); onComplete() }
    }

    fun markRead(otherUid: String) {
        val me = auth.currentUser?.uid ?: return
        db.collection("chats").document(chatId(otherUid)).collection("messages")
            .whereNotEqualTo("senderId", me).get().addOnSuccessListener { result ->
                result.documents.forEach { doc ->
                    doc.reference.update("readBy", FieldValue.arrayUnion(me))
                }
            }
    }

    fun observeMyChats(onChange: (List<String>) -> Unit, onError: (String) -> Unit): () -> Unit {
        val me = auth.currentUser?.uid ?: return {}
        val registration = db.collection("chats").whereArrayContains("members", me)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error.localizedMessage ?: "تعذر تحميل المحادثات."); return@addSnapshotListener }
                onChange(snapshot?.documents.orEmpty().flatMap { it.get("members") as? List<String> ?: emptyList() }
                    .filter { it != me }.distinct())
            }
        return { registration.remove() }
    }

    private fun Boolean?.orFalse() = this ?: false
}
