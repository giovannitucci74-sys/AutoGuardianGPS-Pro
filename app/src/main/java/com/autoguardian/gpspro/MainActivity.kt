package com.autoguardian.gpspro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autoguardian.gpspro.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DateFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("autoguardian", MODE_PRIVATE) }
    private val database by lazy {
        FirebaseDatabase.getInstance("https://autoguardiangps-default-rtdb.europe-west1.firebasedatabase.app")
    }
    private val locationRef by lazy { database.reference.child("vehicles/default/location") }
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var listener: ValueEventListener? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) startTracker() else
                binding.statusText.text = "Permesso posizione non concesso."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.trackerModeButton.setOnClickListener { selectTrackerMode() }
        binding.controlModeButton.setOnClickListener { selectControlMode() }
        binding.startButton.setOnClickListener { ensurePermissionsAndStart() }
        binding.stopButton.setOnClickListener { stopTracker() }
        binding.mapButton.setOnClickListener { openMap() }
        when (prefs.getString("mode", null)) {
            "tracker" -> selectTrackerMode()
            "control" -> selectControlMode()
        }
    }

    private fun selectTrackerMode() {
        prefs.edit().putString("mode", "tracker").apply()
        stopListening()
        binding.modeText.text = "MODALITÀ: TRACKER AUTO"
        binding.statusText.text = if (prefs.getBoolean("tracking", false))
            "Tracking attivo: invio posizione a Firebase."
        else "Premi ATTIVA TRACKER e lascia questo telefono nell’auto."
        binding.startButton.visibility = View.VISIBLE
        binding.stopButton.visibility = View.VISIBLE
        binding.mapButton.visibility = View.GONE
    }

    private fun selectControlMode() {
        prefs.edit().putString("mode", "control").apply()
        binding.modeText.text = "MODALITÀ: TELEFONO DI CONTROLLO"
        binding.statusText.text = "Connessione alla posizione dell’auto..."
        binding.startButton.visibility = View.GONE
        binding.stopButton.visibility = View.GONE
        binding.mapButton.visibility = View.VISIBLE
        listenForCar()
    }

    private fun ensurePermissionsAndStart() {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fine) startTracker() else {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startTracker() {
        prefs.edit().putBoolean("tracking", true).apply()
        ContextCompat.startForegroundService(this, Intent(this, TrackerService::class.java))
        binding.statusText.text = "Tracking attivo: invio posizione a Firebase."
        Toast.makeText(this, "Tracker Auto attivato", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracker() {
        prefs.edit().putBoolean("tracking", false).apply()
        stopService(Intent(this, TrackerService::class.java))
        binding.statusText.text = "Tracker disattivato."
    }

    private fun listenForCar() {
        stopListening()
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                latitude = snapshot.child("latitude").getValue(Double::class.java)
                longitude = snapshot.child("longitude").getValue(Double::class.java)
                val time = snapshot.child("timestamp").getValue(Long::class.java)
                if (latitude != null && longitude != null) {
                    val formattedTime = time?.let {
                        DateFormat.getDateTimeInstance().format(it)
                    } ?: "-"
                    binding.statusText.text =
                        "Auto localizzata\nLatitudine: %.6f\nLongitudine: %.6f\nAggiornamento: %s"
                            .format(latitude, longitude, formattedTime)
                } else binding.statusText.text = "In attesa della prima posizione dell’auto."
            }
            override fun onCancelled(error: DatabaseError) {
                binding.statusText.text = "Firebase: " + error.message
            }
        }
        locationRef.addValueEventListener(listener!!)
    }

    private fun openMap() {
        val lat = latitude
        val lon = longitude
        if (lat == null || lon == null) {
            Toast.makeText(this, "Posizione auto non ancora disponibile.", Toast.LENGTH_LONG).show()
            return
        }
        val uri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(AutoGuardian)")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun stopListening() {
        listener?.let { locationRef.removeEventListener(it) }
        listener = null
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}
