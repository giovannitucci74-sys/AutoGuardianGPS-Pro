package com.autoguardian.gpspro

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.database.*

class TrackerService : Service() {
    private lateinit var client: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private val database by lazy {
        FirebaseDatabase.getInstance("https://autoguardiangps-default-rtdb.europe-west1.firebasedatabase.app")
    }
    private val vehicleRef by lazy { database.reference.child("vehicles/default") }
    private val locationRef by lazy { vehicleRef.child("location") }
    private val historyRef by lazy { vehicleRef.child("history") }
    private var armed = false
    private var safeLat: Double? = null
    private var safeLon: Double? = null
    private var safeRadius = 150.0
    private var baseline: Location? = null
    private var lastAlertAt = 0L
    private var configListener: ValueEventListener? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(100, createNotification("Protezione e invio posizione attivi"))
        listenConfiguration()
        client = LocationServices.getFusedLocationProviderClient(this)
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { processLocation(it) }
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15_000L)
            .setMinUpdateIntervalMillis(10_000L).build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun listenConfiguration() {
        configListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wasArmed = armed
                armed = snapshot.child("armed").getValue(Boolean::class.java) ?: false
                safeLat = snapshot.child("geofence/latitude").getValue(Double::class.java)
                safeLon = snapshot.child("geofence/longitude").getValue(Double::class.java)
                safeRadius = snapshot.child("geofence/radiusMeters").getValue(Double::class.java) ?: 150.0
                if (armed && !wasArmed) baseline = null
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        vehicleRef.addValueEventListener(configListener!!)
    }

    private fun processLocation(location: Location) {
        upload(location)
        if (!armed || location.accuracy > 80f) return
        if (baseline == null) baseline = Location(location)
        val moved = baseline?.distanceTo(location)?.let { it >= 35f } ?: false
        val outside = safeLat?.let { lat ->
            safeLon?.let { lon ->
                val result = FloatArray(1)
                Location.distanceBetween(lat, lon, location.latitude, location.longitude, result)
                result[0] > safeRadius
            }
        } ?: false
        when {
            outside -> sendAlert("GEOFENCE", "L’auto è uscita dalla zona sicura", location)
            moved -> sendAlert("MOVIMENTO", "Movimento dell’auto rilevato", location)
        }
    }

    private fun sendAlert(type: String, message: String, location: Location) {
        val now = System.currentTimeMillis()
        if (now - lastAlertAt < 60_000) return
        lastAlertAt = now
        val alert = mapOf(
            "id" to now, "type" to type, "message" to message,
            "latitude" to location.latitude, "longitude" to location.longitude,
            "timestamp" to now
        )
        vehicleRef.child("alerts/latest").setValue(alert)
        vehicleRef.child("alerts/history").child(now.toString()).setValue(alert)
        getSystemService(NotificationManager::class.java).notify(
            101, createNotification(message)
        )
    }

    private fun upload(location: Location) {
        val timestamp = System.currentTimeMillis()
        val payload = mapOf(
            "latitude" to location.latitude, "longitude" to location.longitude,
            "accuracy" to location.accuracy.toDouble(), "speed" to location.speed.toDouble(),
            "battery" to batteryPercent(), "timestamp" to timestamp
        )
        locationRef.setValue(payload)
        vehicleRef.child("onlineAt").setValue(timestamp)
        historyRef.child(timestamp.toString()).setValue(payload).addOnSuccessListener {
            historyRef.orderByKey().limitToLast(201).get().addOnSuccessListener { snapshot ->
                if (snapshot.childrenCount > 200) snapshot.children.firstOrNull()?.ref?.removeValue()
            }
        }
    }

    private fun batteryPercent(): Int {
        val status = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) level * 100 / scale else -1
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("tracker", "AutoGuardian GPS", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun createNotification(text: String): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "tracker")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("AutoGuardian GPS Pro").setContentText(text)
            .setContentIntent(pending).setOngoing(text.contains("attivi")).setAutoCancel(!text.contains("attivi"))
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        if (::client.isInitialized && ::callback.isInitialized) client.removeLocationUpdates(callback)
        configListener?.let { vehicleRef.removeEventListener(it) }
        vehicleRef.child("onlineAt").setValue(0)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
