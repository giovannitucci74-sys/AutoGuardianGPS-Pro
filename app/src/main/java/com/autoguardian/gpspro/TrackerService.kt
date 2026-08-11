package com.autoguardian.gpspro

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class TrackerService : Service() {
    private lateinit var client: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private val locationRef by lazy {
        FirebaseDatabase.getInstance(
            "https://autoguardiangps-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference.child("vehicles/default/location")
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(100, createNotification())
        client = LocationServices.getFusedLocationProviderClient(this)
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { upload(it) }
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 15_000L
        ).setMinUpdateIntervalMillis(10_000L).build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun upload(location: Location) {
        locationRef.setValue(mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy.toDouble(),
            "speed" to location.speed.toDouble(),
            "timestamp" to System.currentTimeMillis()
        ))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("tracker", "AutoGuardian GPS", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun createNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "tracker")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("AutoGuardian GPS Pro")
            .setContentText("Invio posizione dell’auto attivo")
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        if (::client.isInitialized && ::callback.isInitialized) {
            client.removeLocationUpdates(callback)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
