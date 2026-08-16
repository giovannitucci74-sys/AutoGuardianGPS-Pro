package com.autoguardian.gpspro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autoguardian.gpspro.databinding.ActivityMainBinding
import com.google.firebase.database.*
import java.text.DateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("autoguardian", MODE_PRIVATE) }
    private val database by lazy {
        FirebaseDatabase.getInstance("https://autoguardiangps-default-rtdb.europe-west1.firebasedatabase.app")
    }
    private val vehicleRef by lazy { database.reference.child("vehicles/default") }
    private val locationRef by lazy { vehicleRef.child("location") }
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var timestamp: Long = 0
    private var battery: Int = -1
    private var listener: ValueEventListener? = null
    private var lastMapLatitude: Double? = null
    private var lastMapLongitude: Double? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) startTracker() else binding.statusText.text = "Permesso posizione non concesso."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.liveMap.webViewClient = WebViewClient()
        binding.liveMap.settings.javaScriptEnabled = false
        binding.liveMap.settings.builtInZoomControls = true
        binding.liveMap.settings.displayZoomControls = false
        binding.trackerModeButton.setOnClickListener { selectTrackerMode() }
        binding.controlModeButton.setOnClickListener { selectControlMode() }
        binding.startButton.setOnClickListener { ensurePermissionsAndStart() }
        binding.stopButton.setOnClickListener { stopTracker() }
        binding.positionButton.setOnClickListener { showPosition() }
        binding.mapButton.setOnClickListener { openExternalMap() }
        binding.antitheftButton.setOnClickListener { toggleAntitheft() }\n        binding.geofenceButton.setOnClickListener { setSafeZone() }
        binding.historyButton.setOnClickListener { loadHistory() }
        binding.hiddenPhoneButton.setOnClickListener { showHiddenPhoneStatus() }
        when (prefs.getString("mode", null)) {
            "tracker" -> selectTrackerMode()
            "control" -> selectControlMode()
        }
    }

    private fun setControlButtons(visible: Boolean) {
        val state = if (visible) View.VISIBLE else View.GONE
        binding.positionButton.visibility = state
        binding.antitheftButton.visibility = state
        binding.geofenceButton.visibility = state
        binding.historyButton.visibility = state
        binding.hiddenPhoneButton.visibility = state
        binding.mapButton.visibility = state
    }

    private fun selectTrackerMode() {
        stopService(Intent(this, ControlAlertService::class.java))
        prefs.edit().putString("mode", "tracker").apply()
        stopListening()
        binding.modeText.text = "MODALITÀ: TELEFONO NASCOSTO"
        binding.statusText.text = if (prefs.getBoolean("tracking", false))
            "Tracker attivo: posizione, batteria e stato online vengono inviati."
        else "Premi ATTIVA TRACKER e lascia questo telefono nell’auto."
        binding.startButton.visibility = View.VISIBLE
        binding.stopButton.visibility = View.VISIBLE
        binding.liveMap.visibility = View.GONE
        binding.historyText.visibility = View.GONE
        setControlButtons(false)
    }

    private fun selectControlMode() {
        prefs.edit().putString("mode", "control").apply()
        binding.modeText.text = "MODALITÀ: TELEFONO DI CONTROLLO"
        binding.statusText.text = "Connessione al telefono nascosto..."
        binding.startButton.visibility = View.GONE
        binding.stopButton.visibility = View.GONE
        binding.liveMap.visibility = View.VISIBLE
        binding.historyText.visibility = View.GONE
        setControlButtons(true)
        ContextCompat.startForegroundService(this, Intent(this, ControlAlertService::class.java))
        listenForCar()
    }

    private fun ensurePermissionsAndStart() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (fine) startTracker() else {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startTracker() {
        prefs.edit().putBoolean("tracking", true).apply()
        vehicleRef.child("armed").setValue(true)
        ContextCompat.startForegroundService(this, Intent(this, TrackerService::class.java))
        binding.statusText.text = "Tracker e antifurto attivi."
        Toast.makeText(this, "AutoGuardian attivato", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracker() {
        prefs.edit().putBoolean("tracking", false).apply()
        vehicleRef.child("armed").setValue(false)
        stopService(Intent(this, TrackerService::class.java))
        binding.statusText.text = "Tracker disattivato."
    }

    private fun listenForCar() {
        stopListening()
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                latitude = snapshot.child("latitude").getValue(Double::class.java)
                longitude = snapshot.child("longitude").getValue(Double::class.java)
                timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0
                battery = snapshot.child("battery").getValue(Int::class.java) ?: -1
                val lat = latitude
                val lon = longitude
                if (lat != null && lon != null) {
                    val updated = DateFormat.getDateTimeInstance().format(timestamp)
                    val online = System.currentTimeMillis() - timestamp < 120_000
                    binding.statusText.text =
                        "Auto localizzata • ${if (online) "ONLINE" else "NON AGGIORNATA"}\n" +
                        "Ultimo aggiornamento: $updated\nBatteria: ${if (battery >= 0) "$battery%" else "—"}"
                    updateLiveMap(lat, lon)
                } else binding.statusText.text = "In attesa della prima posizione dell’auto."
            }
            override fun onCancelled(error: DatabaseError) {
                binding.statusText.text = "Firebase: " + error.message
            }
        }
        locationRef.addValueEventListener(listener!!)
    }

    private fun showPosition() {
        val lat = latitude
        val lon = longitude
        if (lat == null || lon == null) {
            Toast.makeText(this, "Posizione non ancora disponibile.", Toast.LENGTH_LONG).show()
        } else {
            binding.statusText.text = "Posizione auto\nLatitudine: %.6f\nLongitudine: %.6f".format(lat, lon)
            updateLiveMap(lat, lon)
        }
    }

    private fun toggleAntitheft() {
        vehicleRef.child("armed").get().addOnSuccessListener {
            val armed = it.getValue(Boolean::class.java) ?: false
            vehicleRef.child("armed").setValue(!armed)
            Toast.makeText(this, if (!armed) "Antifurto attivato" else "Antifurto disattivato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSafeZone() {
        val lat = latitude
        val lon = longitude
        if (lat == null || lon == null) {
            Toast.makeText(this, "Attendi prima la posizione dell’auto.", Toast.LENGTH_LONG).show()
            return
        }
        vehicleRef.child("geofence").setValue(
            mapOf("latitude" to lat, "longitude" to lon, "radiusMeters" to 150.0)
        ).addOnSuccessListener {
            Toast.makeText(this, "Zona sicura impostata: raggio 150 metri", Toast.LENGTH_LONG).show()
        }
    }

    private fun showHiddenPhoneStatus() {
        val online = timestamp > 0 && System.currentTimeMillis() - timestamp < 120_000
        binding.statusText.text =
            "Telefono nascosto: ${if (online) "ONLINE" else "NON AGGIORNATO"}\n" +
            "Batteria: ${if (battery >= 0) "$battery%" else "—"}\n" +
            "Ultimo contatto: ${if (timestamp > 0) DateFormat.getDateTimeInstance().format(timestamp) else "—"}"
    }

    private fun loadHistory() {
        vehicleRef.child("history").orderByKey().limitToLast(20).get()
            .addOnSuccessListener { snapshot ->
                val rows = snapshot.children.mapNotNull { item ->
                    val lat = item.child("latitude").getValue(Double::class.java)
                    val lon = item.child("longitude").getValue(Double::class.java)
                    val time = item.child("timestamp").getValue(Long::class.java)
                    if (lat != null && lon != null && time != null)
                        "${DateFormat.getDateTimeInstance().format(time)}  %.5f, %.5f".format(lat, lon)
                    else null
                }.reversed()
                binding.historyText.text = if (rows.isEmpty()) "Cronologia non ancora disponibile."
                    else rows.joinToString("\n")
                binding.historyText.visibility = View.VISIBLE
            }
            .addOnFailureListener { binding.historyText.text = "Errore cronologia: ${it.message}"; binding.historyText.visibility = View.VISIBLE }
    }

    private fun updateLiveMap(lat: Double, lon: Double) {
        if (lat == lastMapLatitude && lon == lastMapLongitude) return
        lastMapLatitude = lat
        lastMapLongitude = lon
        val delta = 0.003
        val url = "https://www.openstreetmap.org/export/embed.html?bbox=" +
            "${fmt(lon-delta)}%2C${fmt(lat-delta)}%2C${fmt(lon+delta)}%2C${fmt(lat+delta)}" +
            "&layer=mapnik&marker=${fmt(lat)}%2C${fmt(lon)}"
        binding.liveMap.loadUrl(url)
    }

    private fun fmt(value: Double) = String.format(Locale.US, "%.6f", value)

    private fun openExternalMap() {
        val lat = latitude
        val lon = longitude
        if (lat == null || lon == null) {
            Toast.makeText(this, "Posizione non ancora disponibile.", Toast.LENGTH_LONG).show()
            return
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(AutoGuardian)")))
    }

    private fun stopListening() {
        listener?.let { locationRef.removeEventListener(it) }
        listener = null
    }

    override fun onDestroy() {
        stopListening()
        binding.liveMap.stopLoading()
        binding.liveMap.destroy()
        super.onDestroy()
    }
}
