package com.JoaquinApp.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import coil.load
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.fragment.app.FragmentActivity

import com.google.android.material.bottomsheet.BottomSheetDialog

import com.google.firebase.firestore.FirebaseFirestore

import androidx.core.net.toUri

class HomeHandler(private val context: Context, rootView: View) {

    private var isFlashlightOn = false
    private val btnFlashlight: View? = rootView.findViewById(R.id.btnFlashlight)
    private val btnGuides: View? = rootView.findViewById(R.id.btnGuides)
    private val btnRecord: View? = rootView.findViewById(R.id.btnRecord)
    private val ivFlashlight: ImageView? = rootView.findViewById(R.id.ivFlashlight)
    
    private val btnPolice: View? = rootView.findViewById(R.id.btnPolice)
    private val btnFire: View? = rootView.findViewById(R.id.btnFire)
    private val btnHospital: View? = rootView.findViewById(R.id.btnHospital)
    private val btnCivilDefense: View? = rootView.findViewById(R.id.btnCivilDefense)
    private val btnMe: View? = rootView.findViewById(R.id.btnMe)
    
    private val tvTemperature: TextView? = rootView.findViewById(R.id.tvTemperature)
    private val tvWeatherDesc: TextView? = rootView.findViewById(R.id.tvWeatherDesc)
    private val tvExtraWeather: TextView? = rootView.findViewById(R.id.tvExtraWeather)
    private val ivWeatherIcon: ImageView? = rootView.findViewById(R.id.ivWeatherIcon)
    
    private val db = FirebaseFirestore.getInstance()
    private val emergencyNumbers = mutableMapOf<String, String>()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val weatherService = retrofit.create(WeatherService::class.java)
    private val apiKey = "3ab5d8389edf5adf25fe0ca2e5b96997" 

    fun setupHome() {
        btnFlashlight?.setOnClickListener {
            toggleFlashlight()
        }

        btnGuides?.setOnClickListener {
            showGuides()
        }

        btnRecord?.setOnClickListener {
            val intent = Intent(context, CameraActivity::class.java)
            context.startActivity(intent)
        }
        
        setupEmergencyButtons()
        fetchEmergencyNumbers()
        checkGpsAndFetchWeather()
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showGpsDisabledDialog(message: String) {
        AlertDialog.Builder(context)
            .setTitle("GPS Desactivado")
            .setMessage(message)
            .setPositiveButton("Ajustes") { _, _ ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun checkGpsAndFetchWeather() {
        if (!isGpsEnabled()) {
            showGpsDisabledDialog("El GPS está desactivado. No se puede obtener el clima local. ¿Deseas activarlo?")
            return
        }
        fetchLocationAndWeather()
    }

    private fun setupEmergencyButtons() {
        btnPolice?.setOnClickListener { makeCall("Policia") }
        btnFire?.setOnClickListener { makeCall("Bomberos") }
        btnHospital?.setOnClickListener { makeCall("Ambulancia") }
        btnCivilDefense?.setOnClickListener { makeCall("DefensaCivil") }
        btnMe?.setOnClickListener { makeCall("Familiar") }
    }

    private fun fetchEmergencyNumbers() {
        db.collection("NumsEmergencia").get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val number = doc.getLong("numero")?.toString()
                if (number != null) {
                    emergencyNumbers[doc.id] = number
                }
            }
        }
    }

    private fun makeCall(type: String) {
        val number = emergencyNumbers[type]
        if (number != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$number".toUri()
                }
                context.startActivity(intent)
            } else {
                val activity = context as? FragmentActivity
                activity?.let {
                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.CALL_PHONE), 100)
                }
                
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = "tel:$number".toUri()
                }
                context.startActivity(dialIntent)
            }
        } else {
            Toast.makeText(context, "Número no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGuides() {
        val dialog = BottomSheetDialog(context)
        val view = View.inflate(context, R.layout.layout_guides_bottom_sheet, null)
        dialog.setContentView(view)
        dialog.show()
    }

    private fun fetchLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                getWeather(location.latitude, location.longitude)
            }
        }
    }

    private fun getWeather(lat: Double, lon: Double) {
        if (apiKey == "TU_OPENWEATHER_API_KEY") return

        weatherService.getCurrentWeather(lat, lon, apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    weather?.let {
                        tvTemperature?.text = context.getString(R.string.weather_temp_format, it.main.temp.toInt())
                        tvExtraWeather?.text = context.getString(R.string.weather_extra_format, it.main.feelsLike.toInt(), it.wind.speed.toInt())
                        
                        if (it.weather.isNotEmpty()) {
                            tvWeatherDesc?.text = it.weather[0].description.replaceFirstChar { char -> char.uppercase() }
                            val iconCode = it.weather[0].icon
                            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                            ivWeatherIcon?.load(iconUrl)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                // Silencio en error
            }
        })
    }

    private fun toggleFlashlight() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            
            if (isFlashlightOn) {
                ivFlashlight?.setColorFilter(context.getColor(R.color.accent_warning))
            } else {
                ivFlashlight?.clearColorFilter()
            }
            
        } catch (e: Exception) {
            Toast.makeText(context, "No se puede activar la linterna", Toast.LENGTH_SHORT).show()
        }
    }
}
