package com.wisal.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WisalMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "رسالة جديدة"
        val body = message.notification?.body ?: message.data["body"] ?: "لديك رسالة جديدة في وصال"
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "wisal_messages"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
            val channel = NotificationChannel(channelId, "رسائل وصال", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيهات الرسائل الجديدة"
                setSound(sound, audio)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_wisal)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        try { NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification) }
        catch (_: SecurityException) { }
    }
}
