package com.JoaquinApp.myapplication

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewsHandler(private val context: Context, private val rootView: View) {

    private val rvNews: RecyclerView? = rootView.findViewById(R.id.rvNews)
    private val emptyState: View? = rootView.findViewById(R.id.emptyState)
    private val btnShowGlobal: View? = rootView.findViewById(R.id.btnShowGlobal)
    
    private val disasterEvents = mutableListOf<DisasterEvent>()
    private val allEvents = mutableListOf<DisasterEvent>()
    private lateinit var adapter: NewsAdapter
    private val db = FirebaseFirestore.getInstance()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://eonet.gsfc.nasa.gov/api/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val newsService = retrofit.create(NewsService::class.java)

    fun setupNews() {
        adapter = NewsAdapter(disasterEvents)
        rvNews?.layoutManager = LinearLayoutManager(context)
        rvNews?.adapter = adapter

        btnShowGlobal?.setOnClickListener {
            emptyState?.visibility = View.GONE
            rvNews?.visibility = View.VISIBLE
            showEvents(allEvents)
        }

        checkGpsAndFetchDisasters()
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showGpsDisabledDialog() {
        AlertDialog.Builder(context)
            .setTitle("GPS Desactivado")
            .setMessage("El GPS está desactivado. Es necesario para filtrar noticias locales. ¿Deseas activarlo?")
            .setPositiveButton("Ajustes") { _, _ ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Ver Globales") { _, _ ->
                fetchDisasters(null, null)
            }
            .show()
    }

    private fun checkGpsAndFetchDisasters() {
        if (!isGpsEnabled()) {
            showGpsDisabledDialog()
            return
        }
        fetchLocationAndDisasters()
    }

    private fun fetchLocationAndDisasters() {
        db.collection("Ubicacion")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                var userLat: Double? = null
                var userLng: Double? = null

                if (!documents.isEmpty) {
                    val geoPoint = documents.documents[0].getGeoPoint("posicion")
                    userLat = geoPoint?.latitude
                    userLng = geoPoint?.longitude
                }

                fetchDisasters(userLat, userLng)
            }
            .addOnFailureListener {
                fetchDisasters(null, null)
            }
    }

    private fun fetchDisasters(userLat: Double?, userLng: Double?) {
        newsService.getNaturalDisasters().enqueue(object : Callback<EonetResponse> {
            override fun onResponse(call: Call<EonetResponse>, response: Response<EonetResponse>) {
                if (response.isSuccessful) {
                    val events = response.body()?.events ?: emptyList()
                    allEvents.clear()
                    allEvents.addAll(events)
                    
                    if (userLat != null && userLng != null) {
                        filterAndShowEvents(events, userLat, userLng)
                    } else {
                        showEvents(events)
                    }
                }
            }

            override fun onFailure(call: Call<EonetResponse>, t: Throwable) {
                // Network error
            }
        })
    }

    private fun filterAndShowEvents(events: List<DisasterEvent>, userLat: Double, userLng: Double) {
        val filteredEvents = events.filter { event ->
            if (event.geometry.isNotEmpty()) {
                val coords = event.geometry[0].coordinates
                if (coords.size >= 2) {
                    val eventLng = coords[0]
                    val eventLat = coords[1]
                    
                    val results = FloatArray(1)
                    Location.distanceBetween(userLat, userLng, eventLat, eventLng, results)
                    val distanceInKm = results[0] / 1000
                    
                    distanceInKm < 500
                } else false
            } else false
        }
        
        if (filteredEvents.isEmpty() && events.isNotEmpty()) {
            emptyState?.visibility = View.VISIBLE
            rvNews?.visibility = View.GONE
        } else {
            emptyState?.visibility = View.GONE
            rvNews?.visibility = View.VISIBLE
            showEvents(filteredEvents)
        }
    }

    private fun showEvents(newEvents: List<DisasterEvent>) {
        disasterEvents.clear()
        disasterEvents.addAll(newEvents)
        adapter.notifyDataSetChanged()
    }
}
