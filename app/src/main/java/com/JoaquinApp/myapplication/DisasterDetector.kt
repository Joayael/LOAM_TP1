package com.JoaquinApp.myapplication

import android.app.Activity
import android.content.Context
import android.location.Location
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class DisasterDetector(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val alertedDisasters = mutableSetOf<String>()

    fun startMonitoring() {
        db.collection("Catastrofe")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val now = Calendar.getInstance()
                for (doc in snapshot.documents) {
                    if (alertedDisasters.contains(doc.id)) continue

                    val momentoTimestamp = doc.getTimestamp("momento")
                    val lugar = doc.getGeoPoint("lugar")

                    if (momentoTimestamp != null && lugar != null) {
                        val momento = Calendar.getInstance().apply { time = momentoTimestamp.toDate() }

                        val esMismaFecha = now.get(Calendar.YEAR) == momento.get(Calendar.YEAR) &&
                                           now.get(Calendar.DAY_OF_YEAR) == momento.get(Calendar.DAY_OF_YEAR)

                        if (esMismaFecha) {
                            val diff = Math.abs(now.timeInMillis - momento.timeInMillis)
                            if (diff < 900000) { // 15 minutos
                                val disasterLatLng = LatLng(lugar.latitude, lugar.longitude)
                                checkProximityAndAlert(doc.id, disasterLatLng)
                            }
                        }
                    }
                }
            }
    }

    private fun checkProximityAndAlert(disasterId: String, disasterLocation: LatLng) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        disasterLocation.latitude, disasterLocation.longitude,
                        results
                    )

                    val distanceInKm = results[0] / 1000
                    if (distanceInKm < 50) {
                        alertedDisasters.add(disasterId)
                        (context as? MainActivity)?.onDisasterDetected(disasterLocation)
                        triggerAlarm()
                    }
                }
            }
        } catch (e: SecurityException) {
            // Sin permisos
        }
    }

    private fun triggerAlarm() {
        if (ringtone?.isPlaying == true) return

        val alert: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(context, alert)
        ringtone?.play()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500, 1000, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        (context as? Activity)?.runOnUiThread {
            showCriticalDialog()
        }
    }

    private fun showCriticalDialog() {
        AlertDialog.Builder(context)
            .setTitle("⚠️ ¡ALERTA CRÍTICA!")
            .setMessage("¡PELIGRO INMINENTE! Se ha detectado un desastre en tu ubicación actual. Busca refugio inmediatamente. Tu vida está en peligro.")
            .setCancelable(false)
            .setPositiveButton("ENTENDIDO, ESTOY A SALVO") { dialog, _ ->
                stopAlarm()
                dialog.dismiss()
            }
            .show()
    }

    private fun stopAlarm() {
        ringtone?.stop()
        vibrator?.cancel()
    }
}
