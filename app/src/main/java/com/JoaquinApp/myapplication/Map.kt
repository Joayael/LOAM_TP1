package com.JoaquinApp.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions

class MapHandler(private val context: Context, rootView: View) {
    
    private val mapView: MapView? = rootView.findViewById(R.id.googleMapView)
    private val fabMyLocation: FloatingActionButton? = rootView.findViewById(R.id.fabMyLocation)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private val requestLocationPermission = 1
    private val db = FirebaseFirestore.getInstance()

    fun setupMap(savedInstanceState: Bundle?) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { map ->
            googleMap = map
            enableMyLocation()
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            
            // Paso 1: Intentar cargar la última posición guardada en Firebase
            loadLastPositionFromFirebase()
            
            // Paso 2: Verificar si hay un desastre activo reportado por la actividad
            (context as? MainActivity)?.activeDisasterLocation?.let { disaster ->
                showDisasterOnMap(disaster)
            }
        }

        fabMyLocation?.setOnClickListener {
            checkPermissionsAndGetLocation()
        }
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showGpsDisabledDialog() {
        AlertDialog.Builder(context)
            .setTitle("GPS Desactivado")
            .setMessage("El GPS es necesario para ubicarte en el mapa. ¿Deseas activarlo ahora?")
            .setPositiveButton("Ajustes") { _, _ ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun showDisasterOnMap(location: LatLng) {
        googleMap?.let { map ->
            map.clear() // Limpia marcadores anteriores
            map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("⚠️ ZONA DE DESASTRE")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
        }
    }

    private fun loadLastPositionFromFirebase() {
        db.collection("Ubicacion")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val geoPoint = doc.getGeoPoint("posicion")
                    if (geoPoint != null) {
                        val lastLatLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 15f))
                    }
                } else {
                    // Si no hay datos, usar posición por defecto (Buenos Aires)
                    val defaultPos = LatLng(-34.6037, -58.3816)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 10f))
                }
            }
            .addOnFailureListener {
                // Falló la conexión, usar posición por defecto
                val defaultPos = LatLng(-34.6037, -58.3816)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 10f))
            }
    }

    private fun savePositionToFirebase(lat: Double, lng: Double) {
        val data = hashMapOf(
            "posicion" to GeoPoint(lat, lng),
            "fecha" to Timestamp.now(),
        )
        
        db.collection("Ubicacion")
            .add(data)
            .addOnSuccessListener {
                // Posición guardada con éxito
            }
    }

    fun onPermissionGranted() {
        enableMyLocation()
        getUserLocation()
    }

    private fun checkPermissionsAndGetLocation() {
        if (!isGpsEnabled()) {
            showGpsDisabledDialog()
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val activity = context as? FragmentActivity
            activity?.let {
                ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestLocationPermission)
            }
        } else {
            getUserLocation()
        }
    }

    private fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    updateMapAndSave(location.latitude, location.longitude)
                } else {
                    requestFreshLocation()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error de permisos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val priority = Priority.PRIORITY_HIGH_ACCURACY
        fusedLocationClient.getCurrentLocation(priority, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateMapAndSave(location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "No se pudo obtener la ubicación. Verifica tu GPS.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateMapAndSave(lat: Double, lng: Double) {
        val currentLatLng = LatLng(lat, lng)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        savePositionToFirebase(lat, lng)
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            googleMap?.isMyLocationEnabled = true
        }
    }
}
