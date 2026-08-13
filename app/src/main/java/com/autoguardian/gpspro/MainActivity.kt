package com.autoguardian.gpspro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autoguardian.gpspro.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DateFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val database by lazy {
        FirebaseDatabase.getInstance("https://autoguardiangps-default-rtdb.europe-west1.firebasedatabase.app")
    }
    private val locationRef by lazy { database.reference.child("vehicles/default/location") }
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var listener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapButton.setOnClickListener { openMap() }
        listenForCar()
    }

    private fun listenForCar() {
        binding.statusText.text = "Connessione alla posizione dell’auto..."
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                latitude = snapshot.child("latitude").getValue(Double::class.java)
                longitude = snapshot.child("longitude").getValue(Double::class.java)
                val time = snapshot.child("timestamp").getValue(Long::class.java)
                if (latitude != null && longitude != null) {
                    val formattedTime = time?.let { DateFormat.getDateTimeInstance().format(it) } ?: "-"
                    binding.statusText.text =
                        "Auto localizzata\n\nLatitudine: %.6f\nLongitudine: %.6f\n\nUltimo aggiornamento: %s"
                            .format(latitude, longitude, formattedTime)
                    binding.mapButton.isEnabled = true
                } else {
                    binding.statusText.text = "In attesa della prima posizione dell’auto."
                    binding.mapButton.isEnabled = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.statusText.text = "Connessione Firebase: ${error.message}"
                binding.mapButton.isEnabled = false
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
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(AutoGuardian)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        else Toast.makeText(this, "Nessuna app mappe disponibile.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        listener?.let { locationRef.removeEventListener(it) }
        listener = null
        super.onDestroy()
    }
}
