package it.autoguardian.gps

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private val permissionRequest = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createUi()
        requestPermissionsIfNeeded()
        refreshStatus()
    }

    private fun createUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        root.addView(TextView(this).apply {
            text = "AUTOGUARDIAN GPS PRO"
            textSize = 25f
        })

        root.addView(TextView(this).apply {
            text = "Tracker GPS per veicolo proprio o autorizzato"
            textSize = 15f
            setPadding(0, 12, 0, 24)
        })

        status = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, 24)
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "ATTIVA TRACKING"
            setOnClickListener { arm() }
        })

        root.addView(Button(this).apply {
            text = "DISATTIVA"
            setOnClickListener { disarm() }
        })

        root.addView(Button(this).apply {
            text = "ULTIMA POSIZIONE"
            setOnClickListener { showLastLocation() }
        })

        root.addView(TextView(this).apply {
            text = "\nVersione 1.0 Debug\n• GPS in Foreground Service\n• aggiornamento circa ogni 15 secondi\n• ultima posizione salvata localmente\n• ripristino dopo riavvio se tracking attivo"
            setPadding(0, 24, 0, 0)
        })

        setContentView(root)
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), permissionRequest)
    }

    private fun arm() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Concedi il permesso di localizzazione.", Toast.LENGTH_LONG).show()
            return
        }

        getSharedPreferences("settings", MODE_PRIVATE)
            .edit().putBoolean("armed", true).apply()

        ContextCompat.startForegroundService(this, Intent(this, TrackerService::class.java))
        refreshStatus()
    }

    private fun disarm() {
        getSharedPreferences("settings", MODE_PRIVATE)
            .edit().putBoolean("armed", false).apply()
        stopService(Intent(this, TrackerService::class.java))
        refreshStatus()
    }

    private fun refreshStatus() {
        val armed = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("armed", false)
        status.text = if (armed) "STATO: TRACKING ATTIVO" else "STATO: DISATTIVATO"
    }

    private fun showLastLocation() {
        val value = getSharedPreferences("tracker", MODE_PRIVATE)
            .getString("last_location", null)
        if (value == null) {
            Toast.makeText(this, "Nessuna posizione disponibile.", Toast.LENGTH_LONG).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Ultima posizione")
                .setMessage(value)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
