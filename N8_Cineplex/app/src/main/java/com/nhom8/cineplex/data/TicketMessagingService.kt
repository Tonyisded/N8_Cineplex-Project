package com.nhom8.cineplex.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nhom8.cineplex.BuildConfig
import com.nhom8.cineplex.MainActivity
import com.nhom8.cineplex.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

object TicketPush {
    fun initialize(context: Context): Boolean {
        val fields = listOf(BuildConfig.FIREBASE_APPLICATION_ID, BuildConfig.FIREBASE_API_KEY, BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.FIREBASE_SENDER_ID)
        if (fields.any { it.isBlank() }) return false
        if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId(fields[0]).setApiKey(fields[1]).setProjectId(fields[2]).setGcmSenderId(fields[3]).build())
        return true
    }
    fun sync(context: Context, account: ApiAccountRepository) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!initialize(context)) return
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            // Retry on each foreground; never log the device token.
            Thread { runBlocking(Dispatchers.IO) { withTimeoutOrNull(10_000) { runCatching { account.uploadFcm(token) } } } }.start()
        }
    }
    fun ticketId(value: String?): String? = value?.takeIf { runCatching { UUID.fromString(it).toString() == it.lowercase() }.getOrDefault(false) }
}
class TicketMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        runBlocking(Dispatchers.IO) { withTimeoutOrNull(10_000) { runCatching { ApiAccountRepository(applicationContext).uploadFcm(token) } } }
    }
    override fun onMessageReceived(message: RemoteMessage) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val ticket = TicketPush.ticketId(message.data["ticketId"] ?: message.data["ticket_id"]) ?: return
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("tickets", "Vé và hoàn tiền", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(this, MainActivity::class.java).putExtra("ticketId", ticket).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(this, ticket.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "tickets").setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Vé N8 Cineplex đã cập nhật")
            .setContentText("Mở ứng dụng để xem trạng thái vé và thanh toán mới nhất.")
            .setContentIntent(pending).setAutoCancel(true).build()
        manager.notify(ticket.hashCode(), notification)
    }
}
