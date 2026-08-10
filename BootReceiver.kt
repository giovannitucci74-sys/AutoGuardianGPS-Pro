package it.autoguardian.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val armed = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("armed", false)
        if (armed) {
            ContextCompat.startForegroundService(context, Intent(context, TrackerService::class.java))
        }
    }
}
