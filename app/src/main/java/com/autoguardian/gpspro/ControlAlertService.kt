package com.autoguardian.gpspro

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*

class ControlAlertService : Service() {
    private val alertRef by lazy {
        FirebaseDatabase.getInstance("https://autoguardiangps-default-rtdb.europe-west1.firebasedatabase.app")
            .reference.child("vehicles/default/alerts/latest")
    }
    private var listener: ValueEventListener? = null
    private var lastSeen = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(200, notification("Controllo antifurto attivo", false))
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val id = snapshot.child("id").getValue(Long::class.java) ?: return
                if (lastSeen == 0L) { lastSeen = id; return }
                if (id <= lastSeen) return
                lastSeen = id
                val message = snapshot.child("message").getValue(String::class.java)
                    ?: "Allarme AutoGuardian"
                getSystemService(NotificationManager::class.java).notify(201, notification(message, true))
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        alertRef.addValueEventListener(listener!!)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("alerts", "Allarmi AutoGuardian", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun notification(text: String, alert: Boolean): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(if (alert) "ALLARME AUTOGUARDIAN" else "AutoGuardian controllo")
            .setContentText(text).setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_MAX).setAutoCancel(alert).setOngoing(!alert)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() {
        listener?.let { alertRef.removeEventListener(it) }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
